package ru.coderoom.course.security

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "coderoom.course")
data class CourseProperties(
    val githubPatKey: String,
)
