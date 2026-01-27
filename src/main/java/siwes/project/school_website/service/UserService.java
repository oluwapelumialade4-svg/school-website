package siwes.project.school_website.service;

import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import siwes.project.school_website.entity.Role;
import siwes.project.school_website.entity.User;
import siwes.project.school_website.entity.Department;
import siwes.project.school_website.repository.UserRepository;
import siwes.project.school_website.repository.DepartmentRepository;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final PasswordEncoder passwordEncoder;
    private final Optional<JavaMailSender> mailSender;
    private final Path rootLocation = Paths.get("uploads");

    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    public Optional<User> findByMatricNumber(String matricNumber) {
        return userRepository.findByMatricNumber(matricNumber);
    }

    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    @SuppressWarnings("null")
    public User getUserById(Long id) {
        return userRepository.findById(id).orElseThrow(() -> 
            new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    public User save(User user) {
        return userRepository.save(user);
    }

    public void updatePassword(User user, String password) {
        user.setPassword(passwordEncoder.encode(password));
        userRepository.save(user);
    }

    public void registerUser(User user) {
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        if (user.getRole() == null) {
            user.setRole(Role.STUDENT);
        }
        userRepository.save(user);
    }

    public void updateStudentProfile(User user, String fullName, String email, String phoneNumber, String matricNumber, Integer age, String level, String department, MultipartFile file) throws IOException {
        user.setFullName(fullName);
        user.setAge(age);
        user.setLevel(level);
        Department dept = departmentRepository.findByName(department);
        user.setDepartment(dept);
        user.setEmail(email);
        user.setPhoneNumber(phoneNumber);
        user.setMatricNumber(matricNumber);

        if (file != null && !file.isEmpty()) {
            Files.createDirectories(rootLocation);
            String filename = System.currentTimeMillis() + "_profile_" + file.getOriginalFilename();
            Files.copy(file.getInputStream(), rootLocation.resolve(filename), StandardCopyOption.REPLACE_EXISTING);
            user.setProfilePic(filename);
        }
        userRepository.save(user);
    }

    public void updateLecturerProfile(User user, String fullName, String email, String phoneNumber, MultipartFile file) throws IOException {
        user.setFullName(fullName);
        user.setEmail(email);
        user.setPhoneNumber(phoneNumber);

        if (file != null && !file.isEmpty()) {
            Files.createDirectories(rootLocation);
            String filename = System.currentTimeMillis() + "_profile_" + file.getOriginalFilename();
            Files.copy(file.getInputStream(), rootLocation.resolve(filename), StandardCopyOption.REPLACE_EXISTING);
            user.setProfilePic(filename);
        }
        userRepository.save(user);
    }

    public Resource loadProfilePic(String filename) {
        try {
            Path file = rootLocation.resolve(filename);
            Resource resource = new UrlResource(file.toUri());
            if (resource.exists() || resource.isReadable()) {
                return resource;
            } else {
                throw new RuntimeException("Could not read file: " + filename);
            }
        } catch (MalformedURLException e) {
            throw new RuntimeException("Could not read file: " + filename, e);
        }
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public List<User> getUsersByRole(Role role) {
        return userRepository.findByRole(role);
    }

    public List<User> getStudentsByDepartment(Department department) {
        return userRepository.findByDepartmentAndRole(department, Role.STUDENT);
    }

    public void deleteUser(Long id) {
        Optional.ofNullable(id).ifPresent(userRepository::deleteById);
    }

    public List<Department> getAllDepartments() {
        return departmentRepository.findAll();
    }

    public String generateResetToken(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found with email: " + email));
        
        String token = UUID.randomUUID().toString();
        user.setResetPasswordToken(token);
        user.setResetPasswordTokenExpiry(LocalDateTime.now().plusHours(24));
        userRepository.save(user);
        return token;
    }

    public void sendResetTokenEmail(String email, String resetUrl) {
        if (!mailSender.isPresent()) {
            // Mail is not configured, skip sending email
            return;
        }
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setSubject("Password Reset Request");
        message.setText("To reset your password, click the link below:\n" + resetUrl);
        mailSender.get().send(message);
    }

    public Optional<User> getByResetPasswordToken(String token) {
        return userRepository.findByResetPasswordToken(token)
                .filter(u -> u.getResetPasswordTokenExpiry().isAfter(LocalDateTime.now()));
    }

    public Page<User> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable);
    }

    public Page<User> getUsersByRole(Role role, Pageable pageable) {
        return userRepository.findByRole(role, pageable);
    }
}
