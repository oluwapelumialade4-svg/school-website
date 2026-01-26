-- V2__insert_data.sql

-- Departments
INSERT INTO department (id, name) VALUES (1, 'Computer Science');
INSERT INTO department (id, name) VALUES (2, 'Electrical Engineering');

-- Users (Passwords are 'password' hashed with BCrypt, Admin is 'admin123')
INSERT INTO users (id, username, password, role, full_name, email, department_id) VALUES (101, 'student', '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', 'STUDENT', 'Test Student', 'student@school.com', 1);
INSERT INTO users (id, username, password, role, full_name, email, department_id) VALUES (102, 'lecturer', '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', 'LECTURER', 'Test Lecturer', 'lecturer@school.com', 1);
INSERT INTO users (id, username, password, role, full_name, email, matric_number, department_id) VALUES (103, 'adedayo', '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', 'STUDENT', 'Adedayo Samuel', 'samuel@school.com', 'SCI/2024/001', 1);
INSERT INTO users (id, username, password, role, full_name, email, department_id) VALUES (104, 'admin', '$2a$10$4drkCDPAbVGueJLh3c9o9uJEwG8GDQtJxswIbkMYZ93W8hH8wQaN2', 'ADMIN', 'Admin User', 'admin@school.edu', 1);

-- Courses
INSERT INTO course (id, name, course_code, credit_units, department_id, lecturer_id) VALUES (1, 'Intro to Java', 'CSC101', 3, 1, 102);
INSERT INTO course (id, name, course_code, credit_units, department_id, lecturer_id) VALUES (2, 'Spring Boot Advanced', 'CSC404', 4, 1, 102);

-- Assignments
INSERT INTO assignment (id, title, description, created_by_id, department_id, course_id, level) VALUES (1, 'Java Basics', 'Complete the exercises on Chapter 1', 102, 1, 1, '100L');
INSERT INTO assignment (id, title, description, created_by_id, department_id, course_id, level) VALUES (2, 'Spring Boot Portal', 'Complete the SIWES project dashboard', 102, 1, 2, '100L');

-- Reset Sequences to avoid ID conflicts for future inserts
-- This ensures the next ID generated is greater than the max ID currently in the table
SELECT setval(pg_get_serial_sequence('department', 'id'), (SELECT MAX(id) FROM department));
SELECT setval(pg_get_serial_sequence('users', 'id'), (SELECT MAX(id) FROM users));
SELECT setval(pg_get_serial_sequence('course', 'id'), (SELECT MAX(id) FROM course));
SELECT setval(pg_get_serial_sequence('assignment', 'id'), (SELECT MAX(id) FROM assignment));

-- Note: If using H2 for testing, the syntax is slightly different: ALTER TABLE table_name ALTER COLUMN id RESTART WITH (SELECT MAX(id) + 1 FROM table_name);
-- The above syntax is for PostgreSQL.