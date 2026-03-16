package ru.coderoom.runner.executor

import org.springframework.stereotype.Component
import ru.coderoom.runner.messaging.CodeLanguage
import java.nio.file.Path

interface LanguageExecutor {
    fun supports(language: CodeLanguage): Boolean

    fun prepareWorkspace(workspaceDir: Path)
}

@Component
class GoExecutor : LanguageExecutor {
    override fun supports(language: CodeLanguage): Boolean = language == CodeLanguage.GO

    override fun prepareWorkspace(workspaceDir: Path) = Unit
}

@Component
class PythonExecutor : LanguageExecutor {
    override fun supports(language: CodeLanguage): Boolean = language == CodeLanguage.PYTHON

    override fun prepareWorkspace(workspaceDir: Path) = Unit
}

@Component
class JavaExecutor : LanguageExecutor {
    override fun supports(language: CodeLanguage): Boolean = language == CodeLanguage.JAVA

    override fun prepareWorkspace(workspaceDir: Path) = Unit
}
