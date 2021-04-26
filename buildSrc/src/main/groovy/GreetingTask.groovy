import org.gradle.api.tasks.TaskAction
import org.gradle.api.DefaultTask

class GreetingTask extends DefaultTask {
    @TaskAction
    def greet() {
        println 'hello from GreetingTask'
    }
}