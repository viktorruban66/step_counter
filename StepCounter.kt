// StepCounter.kt
import com.beust.jcommander.JCommander
import com.beust.jcommander.Parameter
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import java.io.File
import java.time.LocalDate

data class HistoryEntry(val date: String, val steps: Int)
data class Data(var goal: Int = 10000, val history: MutableList<HistoryEntry> = mutableListOf())

class StepCounter {
    @Parameter(names = ["--add"])
    private var addSteps: Int? = null

    @Parameter(names = ["--show"])
    private var show: Boolean = false

    @Parameter(names = ["--goal"])
    private var goal: Int? = null

    @Parameter(names = ["--history"])
    private var history: Boolean = false

    @Parameter(names = ["--reset"])
    private var reset: Boolean = false

    @Parameter(names = ["--export-csv"])
    private var exportCsv: String? = null

    private val dataFile = "steps.json"
    private val gson = GsonBuilder().setPrettyPrinting().create()
    private val type = object : TypeToken<Data>() {}.type
    private lateinit var data: Data

    init {
        load()
    }

    private fun load() {
        val f = File(dataFile)
        data = if (f.exists()) {
            try {
                val json = f.readText()
                gson.fromJson(json, type) ?: Data()
            } catch (e: Exception) {
                Data()
            }
        } else {
            Data()
        }
        if (data.history == null) data.history = mutableListOf()
    }

    private fun save() {
        File(dataFile).writeText(gson.toJson(data))
    }

    private fun getToday(): HistoryEntry? {
        val today = LocalDate.now().toString()
        return data.history.find { it.date == today }
    }

    private fun addSteps(steps: Int) {
        val entry = getToday()
        if (entry != null) {
            entry.steps += steps
        } else {
            data.history.add(HistoryEntry(LocalDate.now().toString(), steps))
        }
        save()
        val total = entry?.steps ?: steps
        println("\u001B[32mДобавлено $steps шагов. Всего сегодня: $total\u001B[0m")
    }

    private fun show() {
        val entry = getToday()
        val stepsToday = entry?.steps ?: 0
        val progress = (stepsToday * 100 / data.goal).coerceAtMost(100)
        val barLen = 20
        val filled = progress * barLen / 100
        val bar = "█".repeat(filled) + "░".repeat(barLen - filled)
        println("\u001B[36m📊 Сегодня: $stepsToday шагов\u001B[0m")
        println("\u001B[33mЦель: ${data.goal} шагов\u001B[0m")
        println("Прогресс: \u001B[32m$bar\u001B[0m $progress%")
        println("\u001B[35mИстория за 7 дней:\u001B[0m")
        for (i in 6 downTo 0) {
            val d = LocalDate.now().minusDays(i.toLong())
            val dateStr = d.toString()
            val found = data.history.find { it.date == dateStr }
            println("  $dateStr: ${found?.steps ?: 0}")
        }
    }

    private fun setGoal(goal: Int) {
        data.goal = goal
        save()
        println("\u001B[32mЦель установлена: $goal шагов\u001B[0m")
    }

    private fun history() {
        println("\u001B[36mИстория (все записи):\u001B[0m")
        data.history.sortedBy { it.date }.forEach {
            println("  ${it.date}: ${it.steps}")
        }
    }

    private fun reset() {
        data.history.clear()
        save()
        println("\u001B[32mВсе данные сброшены.\u001B[0m")
    }

    private fun exportCsv(filename: String) {
        File(filename).printWriter().use { pw ->
            pw.println("date,steps")
            data.history.forEach { pw.println("${it.date},${it.steps}") }
        }
        println("\u001B[32mЭкспортировано в $filename (CSV)\u001B[0m")
    }

    fun run() {
        when {
            addSteps != null -> addSteps(addSteps!!)
            show -> show()
            goal != null -> setGoal(goal!!)
            history -> history()
            reset -> reset()
            exportCsv != null -> exportCsv(exportCsv!!)
            else -> println("Используйте --help для справки.")
        }
    }
}

fun main(args: Array<String>) {
    val counter = StepCounter()
    JCommander.newBuilder().addObject(counter).build().parse(*args)
    counter.run()
}
