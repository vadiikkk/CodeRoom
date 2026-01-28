package ru.coderoom.course.config

import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration
import ru.coderoom.course.security.JwtProperties
import ru.coderoom.course.security.CourseProperties

@Configuration
@EnableConfigurationProperties(JwtProperties::class, CourseProperties::class)
class CourseServiceConfig
