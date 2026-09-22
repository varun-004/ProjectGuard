-- ProjectGuard Database Initialization

CREATE DATABASE IF NOT EXISTS projectguard
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE projectguard;

-- Engineering Branches
INSERT INTO engineering_branches (name, description)
VALUES
('Computer Science and Engineering',
 'Software, computing, artificial intelligence and information technology'),
('Information Science and Engineering',
 'Information systems, software engineering, data and computing'),
('Electronics and Communication Engineering',
 'Electronics, communication, embedded systems and IoT'),
('Electrical and Electronics Engineering',
 'Electrical systems, automation, power and control'),
('Mechanical Engineering',
 'Mechanical systems, manufacturing, design and robotics'),
('Civil Engineering',
 'Construction, infrastructure, structural and environmental engineering');

-- Project Domains
INSERT INTO project_domains (name, description)
VALUES
('Artificial Intelligence and Machine Learning', 'AI, machine learning and intelligent systems'),
('Web Development', 'Frontend, backend and full-stack web applications'),
('Mobile Application Development', 'Android, iOS and cross-platform applications'),
('Data Science and Analytics', 'Data analysis, visualization and predictive analytics'),
('Cyber Security', 'Network security, application security and threat analysis'),
('Cloud Computing', 'Cloud platforms, distributed systems and DevOps'),
('Internet of Things', 'Connected devices, sensors and IoT systems'),
('Embedded Systems', 'Microcontrollers and embedded applications'),
('VLSI and Digital Systems', 'Digital electronics and VLSI design'),
('Robotics and Automation', 'Robotics, automation and control systems'),
('Electrical Power Systems', 'Power generation, transmission and distribution'),
('Renewable Energy', 'Solar, wind and sustainable energy systems'),
('CAD and Product Design', 'Computer-aided design and mechanical product development'),
('Manufacturing and Production', 'Manufacturing processes and production systems'),
('Structural Engineering', 'Structural analysis and building infrastructure'),
('Construction Technology', 'Construction planning and project technology'),
('Environmental Engineering', 'Environmental monitoring and sustainable infrastructure');