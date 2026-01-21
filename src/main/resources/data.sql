-- Initial Data

-- Departments
INSERT INTO department (id, name) VALUES (1, 'Computer Science');
INSERT INTO department (id, name) VALUES (2, 'Electrical Engineering');

-- Passwords are 'password' hashed with BCrypt
INSERT INTO users (id, username, password, role, full_name, email, department_id) VALUES (101, 'student', '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', 'STUDENT', 'Test Student', 'student@school.com', 1);
INSERT INTO users (id, username, password, role, full_name, email, department_id) VALUES (102, 'lecturer', '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', 'LECTURER', 'Test Lecturer', 'lecturer@school.com', 1);

-- New Student: Adedayo Samuel
INSERT INTO users (id, username, password, role, full_name, email, matric_number, department_id) VALUES (103, 'adedayo', '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', 'STUDENT', 'Adedayo Samuel', 'samuel@school.com', 'SCI/2024/001', 1);

-- New Admin User: admin@school.edu / admin123
INSERT INTO users (id, username, password, role, full_name, email, department_id) VALUES (104, 'admin', '$2a$10$4drkCDPAbVGueJLh3c9o9uJEwG8GDQtJxswIbkMYZ93W8hH8wQaN2', 'ADMIN', 'Admin User', 'admin@school.edu', 1);

-- Courses
INSERT INTO course (id, name, course_code, credit_units, department_id, lecturer_id) VALUES (1, 'Intro to Java', 'CSC101', 3, 1, 102);
INSERT INTO course (id, name, course_code, credit_units, department_id, lecturer_id) VALUES (2, 'Spring Boot Advanced', 'CSC404', 4, 1, 102);

-- Assignments
INSERT INTO assignment (id, title, description, created_by_id, department_id, course_id) VALUES (1, 'Java Basics', 'Complete the exercises on Chapter 1', 102, 1, 1);
INSERT INTO assignment (id, title, description, created_by_id, department_id, course_id) VALUES (2, 'Spring Boot Portal', 'Complete the SIWES project dashboard', 102, 1, 2);