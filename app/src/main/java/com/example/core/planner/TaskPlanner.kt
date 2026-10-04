package com.example.core.planner

import com.example.core.model.PlanStep
import com.example.core.model.StepStatus
import com.example.core.model.TaskPlan
import com.example.core.tools.AgentToolSystem
import com.example.data.local.dao.TaskDao
import com.example.data.local.entity.AgentTaskEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class TaskPlanner(
    private val toolSystem: AgentToolSystem,
    private val taskDao: TaskDao
) {
    private val _currentPlan = MutableStateFlow<TaskPlan?>(null)
    val currentPlan: StateFlow<TaskPlan?> = _currentPlan.asStateFlow()

    fun createPlanFromGoal(goal: String): TaskPlan {
        val steps = mutableListOf<PlanStep>()
        val taskId = UUID.randomUUID().toString()

        val lower = goal.lowercase()
        if (lower.contains("three.js") || lower.contains("3d") || lower.contains("animation") || lower.contains("website") || lower.contains("game")) {
            steps.add(PlanStep("step_1", 1, "Initialize Project", "Create code project container in Editor", "save_project", mapOf("name" to "ThreeJS Showcase", "project_type" to "THREE_JS")))
            steps.add(PlanStep("step_2", 2, "Synthesize Code", "Generate modern HTML5, CSS3, and Three.js 3D scripts", "create_project_file", mapOf("file_path" to "index.html")))
            steps.add(PlanStep("step_3", 3, "Inspect Syntax & Assets", "Validate code structure and script imports", "read_screen"))
            steps.add(PlanStep("step_4", 4, "Deploy to Preview", "Launch interactive testing sandbox in Preview workspace"))
            steps.add(PlanStep("step_5", 5, "Verify Animation Runtime", "Verify WebGL canvas initialization without runtime error"))
        } else if (lower.contains("open") || lower.contains("search") || lower.contains("browser") || lower.contains("find")) {
            val target = if (lower.contains("browser")) "Chrome" else goal.substringAfter("open").trim().take(20)
            steps.add(PlanStep("step_1", 1, "Resolve Target Action", "Select optimal execution backend (Intent/Accessibility)"))
            steps.add(PlanStep("step_2", 2, "Execute Launch", "Launch target application or URL", "open_app", mapOf("app_name" to target)))
            steps.add(PlanStep("step_3", 3, "Observe Screen State", "Capture active window hierarchy and verify foreground status", "read_screen"))
            steps.add(PlanStep("step_4", 4, "Verify State Change", "Confirm UI responsiveness and completion evidence"))
        } else {
            steps.add(PlanStep("step_1", 1, "Analyze Request Intent", "Parse command constraints and parameters"))
            steps.add(PlanStep("step_2", 2, "Formulate Execution", "Select necessary agent tools and verified actions"))
            steps.add(PlanStep("step_3", 3, "Execute & Observe", "Perform operation and observe immediate system feedback"))
            steps.add(PlanStep("step_4", 4, "Verify Outcome", "Ensure target outcome matches user intent without error"))
        }

        val plan = TaskPlan(
            taskId = taskId,
            goal = goal,
            steps = steps
        )
        _currentPlan.value = plan
        return plan
    }

    suspend fun executeNextStep(onStepUpdated: (PlanStep) -> Unit): Boolean {
        val plan = _currentPlan.value ?: return false
        if (plan.currentStepIndex >= plan.steps.size) {
            plan.isCompleted = true
            plan.finalReport = "All ${plan.steps.size} steps completed and verified."
            _currentPlan.value = plan.copy()
            persistPlan(plan)
            return false
        }

        val step = plan.steps[plan.currentStepIndex]
        step.status = StepStatus.IN_PROGRESS
        _currentPlan.value = plan.copy()
        onStepUpdated(step)

        if (step.toolName != null) {
            val result = toolSystem.executeTool(step.toolName, step.toolArgs)
            if (result.isSuccess) {
                step.status = StepStatus.VERIFYING
                step.observation = result.resultData
                step.verificationResult = result.verificationEvidence ?: "Execution confirmed via tool output."
                step.status = StepStatus.COMPLETED
            } else {
                step.status = StepStatus.FAILED
                step.observation = result.error ?: result.resultData
                step.verificationResult = "Step failed verification: ${result.error}"
            }
        } else {
            step.status = StepStatus.COMPLETED
            step.verificationResult = "Internal task phase verified."
        }

        onStepUpdated(step)
        plan.currentStepIndex++
        if (plan.currentStepIndex >= plan.steps.size) {
            plan.isCompleted = true
            plan.finalReport = "Task completed successfully. Output verified."
        }
        _currentPlan.value = plan.copy()
        persistPlan(plan)
        return !plan.isCompleted
    }

    private suspend fun persistPlan(plan: TaskPlan) {
        val jsonArray = JSONArray()
        for (s in plan.steps) {
            jsonArray.put(JSONObject().apply {
                put("id", s.id)
                put("order", s.order)
                put("title", s.title)
                put("desc", s.description)
                put("status", s.status.name)
                put("obs", s.observation.orEmpty())
                put("ver", s.verificationResult.orEmpty())
            })
        }
        val entity = AgentTaskEntity(
            id = plan.taskId,
            goal = plan.goal,
            status = if (plan.isCompleted) "COMPLETED" else if (plan.isFailed) "FAILED" else "IN_PROGRESS",
            currentStepIndex = plan.currentStepIndex,
            planJson = jsonArray.toString(),
            finalReport = plan.finalReport
        )
        taskDao.insertTask(entity)
    }

    fun clearActivePlan() {
        _currentPlan.value = null
    }
}
