package com.dnd.spaced.domain.admin.application;

import com.dnd.spaced.domain.admin.application.dto.request.AdminWordConditionInfoDto;
import com.dnd.spaced.domain.admin.application.dto.request.AdminWordRequestDto;
import com.dnd.spaced.domain.admin.application.dto.response.ReportInfoDto;
import com.dnd.spaced.domain.admin.application.exception.ReportNotFoundException;
import com.dnd.spaced.domain.admin.domain.repository.AdminRepository;
import com.dnd.spaced.domain.admin.domain.repository.AdminRepositoryMapper;
import com.dnd.spaced.domain.admin.domain.repository.dto.request.AdminWordConditionDto;
import com.dnd.spaced.domain.admin.presentation.dto.response.AdminWordResponse;
import com.dnd.spaced.domain.comment.application.exception.CommentNotFoundException;
import com.dnd.spaced.domain.comment.domain.Comment;
import com.dnd.spaced.domain.comment.domain.repository.CommentRepository;
import com.dnd.spaced.domain.report.application.exception.ReportedCommentNotFoundException;
import com.dnd.spaced.domain.report.domain.Report;
import com.dnd.spaced.domain.report.domain.repository.ReportRepository;
import com.dnd.spaced.domain.word.application.exception.WordNotFoundException;
import com.dnd.spaced.domain.word.domain.Word;
import com.dnd.spaced.domain.word.domain.repository.WordRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminService {

    private static final int PAGE_SIZE = 15;

    private final WordRepository wordRepository;
    private final ReportRepository reportRepository;
    private final CommentRepository commentRepository;
    private final AdminRepository adminRepository;

    public List<AdminWordResponse> findAllBy(AdminWordConditionInfoDto dto) {
        AdminWordConditionDto adminWordConditionDto = AdminRepositoryMapper.to(
                dto.categoryName(),
                dto.lastWordName(),
                dto.pageable()
        );
        List<Word> result = adminRepository.findAllBy(adminWordConditionDto);

        return AdminServiceMapper.toAdminWordResponseList(result);
    }

    @Transactional
    public Long createWord(AdminWordRequestDto wordRequestDto) {
        Word word = AdminServiceMapper.fromCreateRequest(wordRequestDto);
        wordRepository.save(word);
        return word.getId();
    }

    @Transactional
    public void deleteWord(Long wordId) {
        Word word = findWordById(wordId);
        wordRepository.delete(word);
    }

    @Transactional
    public void updateWord(Long wordId, AdminWordRequestDto wordRequestDto) {
        Word existingWord = findWordById(wordId);
        AdminServiceMapper.fromUpdateRequest(wordRequestDto, existingWord);
        wordRepository.save(existingWord);
    }

    @Transactional
    public void acceptReport(Long reportId) {
        Report report = getReport(reportId);
        Comment comment = getComment(report.getCommentId());

        commentRepository.delete(comment);
        reportRepository.deleteById(reportId);
    }

    @Transactional
    public void ignoreReport(Long reportId) {
        validateReportExists(reportId);
        deleteReport(reportId);
    }

    @Transactional
    public List<ReportInfoDto> findReports(Long lastReportId) {
        List<Report> reports = reportRepository.findReportsAfterId(lastReportId, PAGE_SIZE);

        return reports.stream()
                .map(AdminServiceMapper::toReportInfoDto)
                .toList();
    }

    public AdminWordResponse getWord(Long wordId) {
        Word word = findWordById(wordId);
        return AdminServiceMapper.toResponseDto(word);
    }

    private Report getReport(Long reportId) {
        return reportRepository.findById(reportId)
                .orElseThrow(ReportedCommentNotFoundException::new);
    }

    private Comment getComment(Long commentId) {
        return commentRepository.findBy(commentId)
                .orElseThrow(CommentNotFoundException::new);
    }

    private Word findWordById(Long wordId) {
        return wordRepository.findBy(wordId)
                .orElseThrow(WordNotFoundException::new);
    }

    private void validateReportExists(Long reportId) {
        if (!reportRepository.existsById(reportId)) {
            throw new ReportNotFoundException();
        }
    }

    private void deleteReport(Long reportId) {
        reportRepository.deleteById(reportId);
    }
}

