package siwes.project.school_website.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import siwes.project.school_website.entity.Assignment;
import siwes.project.school_website.entity.Submission;
import siwes.project.school_website.service.AssignmentService;
import siwes.project.school_website.service.SubmissionService;
import siwes.project.school_website.service.UserService;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class LecturerControllerTest {

    @Mock
    private AssignmentService assignmentService;

    @Mock
    private SubmissionService submissionService;

    @Mock
    private UserService userService;

    @InjectMocks
    private LecturerController lecturerController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(lecturerController).build();
    }

    @Test
    void gradeSubmission_Success() throws Exception {
        Long submissionId = 10L;
        Long assignmentId = 5L;
        Integer grade = 85;
        String feedback = "Excellent analysis.";

        Assignment mockAssignment = new Assignment();
        mockAssignment.setId(assignmentId);

        Submission mockSubmission = new Submission();
        mockSubmission.setId(submissionId);
        mockSubmission.setAssignment(mockAssignment);

        when(submissionService.getSubmissionById(submissionId)).thenReturn(mockSubmission);

        mockMvc.perform(post("/lecturer/submission/{id}/grade", submissionId)
                .param("grade", String.valueOf(grade))
                .param("feedback", feedback))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/lecturer/assignment/" + assignmentId + "/submissions?graded"));

        verify(submissionService, times(1)).gradeSubmission(submissionId, grade, feedback);
        verify(submissionService, times(1)).getSubmissionById(submissionId);
    }
}