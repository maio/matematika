import kotlinx.coroutines.MainScope
import kotlinx.coroutines.await
import kotlinx.coroutines.launch
import kotlinx.css.*
import kotlinx.html.InputType
import kotlinx.html.classes
import kotlinx.html.js.onClickFunction
import kotlinx.html.js.onKeyDownFunction
import org.w3c.dom.HTMLElement
import org.w3c.dom.HTMLInputElement
import react.*
import react.dom.*
import styled.css
import styled.styledInput
import styled.styledP
import styled.styledSpan
import kotlin.browser.document
import kotlin.browser.window
import kotlin.random.Random

/**
 * Exercises:
 *   - Basic       8 - 5 = ?
 *   - Missing     8 - ? = 2
 */

interface ExerciseProps : RProps {
    var exercise: ExerciseState.Pending
    var onResult: (ExerciseState) -> Any
}

fun RBuilder.catPicture() = img {
    attrs.src = "https://cataas.com/cat?${Random.nextInt(0, 10000000)}"
}

val failmojis = listOf("😦","🤭")

val Ticker = functionalComponent<RProps> {
    val (tick, setTick) = useState(0)

    useEffectWithCleanup {
        val timeoutId = window.setTimeout({
            setTick(tick + 1)
        }, 1000)

        return@useEffectWithCleanup {
            window.clearTimeout(timeoutId)
        }
    }

    div { +"Tick: $tick" }
}

data class TodoItem(val userId: Int, val title: String)

val RestTodo = functionalComponent<RProps> {
    val (title, setTitle) = useState("")

    if (title.isBlank()) {
        val mainScope = MainScope()
        mainScope.launch {
            val info =
                window.fetch("https://jsonplaceholder.typicode.com/todos/1")
                    .await()
                    .json()
                    .await()
                    .unsafeCast<TodoItem>()

            setTitle(info.title)
        }
    }

    div { +"Todo Title: $title" }
}

val Input = functionalComponent<RProps> {
    val smiles = listOf(
        "😛",  // 0
        "😜",  // 1
        "😝",  // 2
        "🤪"   // 3
    )
    val (idx, setIdx) = useState(0)

    useEffectWithCleanup(dependencies = emptyList()) {
        val intervalId = window.setInterval({
            setIdx(smiles.indices.random())
        }, 500)

        return@useEffectWithCleanup {
            window.clearInterval(intervalId)
        }
    }

    styledInput {
        css {
            textAlign = TextAlign.center
            fontSize = 550.pct
        }
        attrs {
            size = "2"
            maxLength = "2"
            minLength = "1"
            pattern = "[0-1]+"
            placeholder = smiles[idx]
            type = InputType.text
        }
    }
}

class Exercise(props: ExerciseProps) :
    RComponent<ExerciseProps, RState>(props) {
    lateinit var resultInput: HTMLInputElement

    override fun componentDidMount() {
        resultInput.focus()
    }

    override fun RBuilder.render() {
        val exercise = props.exercise

        div {
            child(Ticker)
            child(RestTodo)
            child(Input)

            attrs.classes = setOf("exercise")

            h1 {
                +"Úkol (máš ${exercise.triesLeft} ${if (exercise.triesLeft == 1) "pokus" else "pokusy"}):"
            }

            styledP {
                key = "exercise"
                css {
                    textAlign = TextAlign.center
                }
                styledSpan {
                    css {
                        color = Color.red
                    }
                    +"${exercise.a}"
                }
                span { +exercise.op.value }
                styledSpan {
                    css {
                        color = Color.green
                    }
                    +"${exercise.b}"
                }
                styledSpan {
                    css {
                        color = Color.blue
                    }
                    +"="
                }
                styledInput {
                    css {
                        textAlign = TextAlign.center
                        fontSize = 550.pct
                    }
                    ref { resultInput = it }
                    attrs {
                        size = "2"
                        maxLength = "2"
                        minLength = "1"
                        pattern = "[0-1]+"
                        placeholder = "?"
                        type = InputType.text
                        onKeyDownFunction = { event ->
                            val key = event.asDynamic().key as String
                            if (key == "Enter") {
                                onSubmit()
                            }
                        }
                    }
                }
            }
        }
    }

    private fun onSubmit() {
        val exercise = props.exercise
        val actual = resultInput.value.toIntOrNull() ?: return

        val next = exercise.submit(actual)

        // No success yet :/
        if (next is ExerciseState.Pending) {
            resultInput.value = failmojis.random()
            window.setTimeout({ resultInput.value = "" }, 1500)
        }

        props.onResult(next)
    }
}

enum class Operation(val value: String) {
    PLUS("+") {
        override fun compute(a: Int, b: Int): Int = a + b
    },
    MINUS("-") {
        override fun compute(a: Int, b: Int): Int = a - b
    };

    abstract fun compute(a: Int, b: Int): Int
}

interface SuccessProps : RProps {
    var onNext: () -> Unit
}

val Success = functionalComponent<SuccessProps> { props ->
    var aRef: HTMLElement? = null

    useEffect(emptyList()) { aRef?.focus() }

    a {
        ref { aRef = it }
        catPicture()
        br { }
        br { }

        attrs.href = "#"
        attrs.onClickFunction = { event ->
            event.preventDefault()
            props.onNext()
        }

        +"Chci další úkol.."
    }
}

interface FailureProps : RProps {
    var onNext: () -> Unit
}

val Failure = functionalComponent<FailureProps> { props ->
    val (idx, setIdx) = useState(0)

    useEffectWithCleanup(listOf(idx)) {
        val id = window.setTimeout({
            if (idx == failmojis.size - 1) {
                props.onNext()
                return@setTimeout
            }

            setIdx(idx + 1)
        }, 1500)

        return@useEffectWithCleanup {
            window.clearTimeout(id)
        }
    }

    styledP {
        css {
            fontSize = 600.pct
        }
        +failmojis[idx]
    }
}

sealed class ExerciseState : RState {
    data class Pending(
        val a: Int,
        val op: Operation,
        val b: Int,
        var triesLeft: Int = 2
    ) : ExerciseState() {
        fun submit(actual: Int): ExerciseState {
            return if (actual == op.compute(a, b)) {
                Success
            } else {
                val triesLeft = this.triesLeft - 1

                if (triesLeft > 0) {
                    copy(triesLeft = triesLeft)
                } else {
                    Failure
                }
            }
        }
    }

    object Success : ExerciseState()
    object Failure : ExerciseState()
}

val Main = functionalComponent<RProps> {
    val a = (4..10).random()
    val b = (0..a).random()
    val op = Operation.MINUS

    val (result, setResult) = useState(
        ExerciseState.Pending(a, op, b) as ExerciseState
    )

    when (result) {
        is ExerciseState.Pending ->
            child(Exercise::class) {
                attrs {
                    this.exercise = result
                    onResult = { setResult(it) }
                }
            }
        is ExerciseState.Success ->
            child(Success) {
                attrs.onNext = {
                    setResult(ExerciseState.Pending(a, op, b))
                }
            }
        ExerciseState.Failure -> {
            child(Failure) {
                attrs.onNext = {
                    setResult(ExerciseState.Pending(a, op, b))
                }
            }
        }
    }.let {}
}

fun main() {
    render(document.getElementById("root")) {
        child(Main)
    }
}
