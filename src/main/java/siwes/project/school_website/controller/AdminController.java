package siwes.project.school_website.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import siwes.project.school_website.entity.Role;
import siwes.project.school_website.entity.User;
import siwes.project.school_website.entity.Department;
import siwes.project.school_website.entity.Course;
import siwes.project.school_website.service.AssignmentService;
import siwes.project.school_website.service.UserService;
import siwes.project.school_website.repository.UserRepository;
import siwes.project.school_website.repository.DepartmentRepository;
import siwes.project.school_website.repository.CourseRepository;
import siwes.project.school_website.repository.AssignmentRepository;
import java.util.List;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.security.Principal;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final UserService userService;
    private final AssignmentService assignmentService;
    private final DepartmentRepository departmentRepository;
    private final CourseRepository courseRepository;
    private final AssignmentRepository assignmentRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @GetMapping("/dashboard")
    public String dashboard(@RequestParam(required = false) Role role, 
                            @RequestParam(defaultValue = "0") int page,
                            @RequestParam(defaultValue = "5") int size,
                            Model model, Principal principal) {
        String username = principal != null ? principal.getName() : "Admin";
        model.addAttribute("username", username);

        // Pass data to the view. You might want to add a method to UserService to fetch all users.
        model.addAttribute("assignments", assignmentService.getAllAssignments());
        model.addAttribute("lecturers", userService.getUsersByRole(Role.LECTURER));
        
        Pageable pageable = PageRequest.of(page, size);
        Page<User> users = (role != null) ? userService.getUsersByRole(role, pageable) : userService.getAllUsers(pageable);
        
        model.addAttribute("users", users);
        
        if (role != null) {
            model.addAttribute("selectedRole", role.name());
        }
        
        model.addAttribute("departments", departmentRepository.findAll());
        model.addAttribute("courses", courseRepository.findAll());

        return "admin/dashboard";
    }

    @GetMapping("/user/delete/{id}")
    public String deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return "redirect:/admin/dashboard";
    }

    @PostMapping("/user/reset-password")
    public String resetUserPassword(@RequestParam Long userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new IllegalArgumentException("Invalid User ID"));
        user.setPassword(passwordEncoder.encode("password123"));
        userRepository.save(user);
        return "redirect:/admin/dashboard?success=passwordReset";
    }

    @PostMapping("/department/create")
    public String createDepartment(@RequestParam String name) {
        Department dept = new Department();
        dept.setName(name);
        departmentRepository.save(dept);
        return "redirect:/admin/dashboard";
    }

    @PostMapping("/course/create")
    @SuppressWarnings("null")
    public String createCourse(@RequestParam String name, 
                               @RequestParam String courseCode, 
                               @RequestParam Integer creditUnits, 
                               @RequestParam Long departmentId) {
        Department dept = departmentRepository.findById(departmentId).orElseThrow();
        Course course = new Course();
        course.setName(name);
        course.setCourseCode(courseCode);
        course.setCreditUnits(creditUnits);
        course.setDepartment(dept);
        courseRepository.save(course);
        return "redirect:/admin/dashboard";
    }

    @PostMapping("/course/update")
    public String updateCourse(@RequestParam Long id, @RequestParam String name, @RequestParam String courseCode, @RequestParam Integer creditUnits) {
        Course course = courseRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Invalid Course ID"));
        course.setName(name);
        course.setCourseCode(courseCode);
        course.setCreditUnits(creditUnits);
        courseRepository.save(course);
        return "redirect:/admin/dashboard?success=courseUpdated";
    }

    @PostMapping("/assign-course")
    public String assignCourse(@RequestParam Long courseId, @RequestParam Long lecturerId) {
        Course course = courseRepository.findById(courseId).orElseThrow(() -> new IllegalArgumentException("Invalid Course ID"));
        User lecturer = userService.getUserById(lecturerId);
        if (lecturer.getRole() != Role.LECTURER) {
            return "redirect:/admin/dashboard?error=InvalidLecturer";
        }
        course.setLecturer(lecturer);
        courseRepository.save(course);
        return "redirect:/admin/dashboard?success=assigned";
    }

    @GetMapping("/assignment/delete/{id}")
    public String deleteAssignment(@PathVariable Long id) {
        if (id == null) {
            throw new IllegalArgumentException("Assignment ID cannot be null");
        }
        assignmentRepository.deleteById(id);
        return "redirect:/admin/dashboard";
    }

    @GetMapping("/course/{id}")
    public String viewCourseDetails(@PathVariable Long id, Model model) {
        if (id == null) {
            throw new IllegalArgumentException("Course ID cannot be null");
        }
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid Course ID"));
        
        List<User> students = userService.getStudentsByDepartment(course.getDepartment());
        
        model.addAttribute("course", course);
        model.addAttribute("students", students);
        return "admin/course-details";
    }

    @GetMapping("/course/{id}/export")
    public void exportCourseStudents(@PathVariable Long id, HttpServletResponse response) throws IOException {
        if (id == null) {
            throw new IllegalArgumentException("Course ID cannot be null");
        }
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid Course ID"));
        
        List<User> students = userService.getStudentsByDepartment(course.getDepartment());

        response.setContentType("text/csv");
        response.setHeader("Content-Disposition", "attachment; filename=\"students_" + course.getName() + ".csv\"");

        try (PrintWriter writer = response.getWriter()) {
            writer.println("Full Name,Matric Number,Level,Email,Phone Number");
            for (User student : students) {
                writer.println(escapeCsv(student.getFullName()) + "," + 
                               escapeCsv(student.getMatricNumber()) + "," + 
                               escapeCsv(student.getLevel()) + "," + 
                               escapeCsv(student.getEmail()) + "," +
                               escapeCsv(student.getPhoneNumber()));
            }
        }
    }

    @GetMapping("/departments")
    public String manageDepartments(Model model) {
        model.addAttribute("departments", departmentRepository.findAll());
        return "admin/manage-departments";
    }

    @PostMapping("/department/save")
    public String saveDepartment(@RequestParam(required = false) Long id, @RequestParam String name) {
        Department dept;
        if (id != null) {
            dept = departmentRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Invalid Department ID"));
        } else {
            dept = new Department();
        }
        dept.setName(name);
        departmentRepository.save(dept);
        return "redirect:/admin/departments?success";
    }

    @GetMapping("/department/delete/{id}")
    public String deleteDepartment(@PathVariable Long id) {
        if (id == null) {
            return "redirect:/admin/departments?error=InvalidID";
        }
        try {
            departmentRepository.deleteById(id);
        } catch (Exception e) {
            return "redirect:/admin/departments?error=ConstraintViolation";
        }
        return "redirect:/admin/departments?success=deleted";
    }

    @GetMapping("/profile")
    public String editProfile(Model model, Principal principal) {
        String username = principal.getName();
        User user = userService.findByUsername(username).orElseThrow();
        model.addAttribute("user", user);
        return "admin/profile";
    }

    @PostMapping("/profile")
    public String updateProfile(@RequestParam String fullName,
                                @RequestParam String email,
                                @RequestParam String phoneNumber,
                                @RequestParam Integer age,
                                @RequestParam(required = false) MultipartFile file,
                                Principal principal) throws IOException {
        String username = principal.getName();
        User user = userService.findByUsername(username).orElseThrow();

        user.setFullName(fullName);
        user.setEmail(email);
        user.setPhoneNumber(phoneNumber);
        user.setAge(age);

        if (file != null && !file.isEmpty()) {
            Files.createDirectories(Paths.get("uploads"));
            String filename = System.currentTimeMillis() + "_profile_" + file.getOriginalFilename();
            Files.copy(file.getInputStream(), Paths.get("uploads").resolve(filename), StandardCopyOption.REPLACE_EXISTING);
            user.setProfilePic(filename);
        }

        userRepository.save(user);
        return "redirect:/admin/profile?success";
    }

    private String escapeCsv(String data) {
        if (data == null) return "";
        String escapedData = data.replaceAll("\"", "\"\"");
        if (data.contains(",") || data.contains("\n") || data.contains("\"")) {
            data = "\"" + escapedData + "\"";
        }
        return data;
    }
}