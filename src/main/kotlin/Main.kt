import kotlinx.coroutines.MainScope
import kotlinx.coroutines.await
import kotlinx.coroutines.launch
import kotlinx.css.*
import kotlinx.html.InputType
import kotlinx.html.classes
import kotlinx.html.js.onChangeFunction
import kotlinx.html.js.onClickFunction
import kotlinx.html.js.onKeyDownFunction
import org.w3c.dom.HTMLElement
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.events.Event
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
    var a: Int
    var op: Operation
    var b: Int
    var onSuccess: () -> Any
    var onFailure: () -> Any
}

interface ExerciseInternalState : RState {
    var triesLeft: Int
    var actual: String
    var success: Boolean
}

fun RBuilder.catPicture() = img {
    attrs.src = "https://cataas.com/cat?${Random.nextInt(0, 10000000)}"
}

val cry = "😿"

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

class Exercise : RComponent<ExerciseProps, ExerciseInternalState>() {
    lateinit var resultInput: HTMLInputElement

    init {
        state.triesLeft = 2
        state.actual = ""
        state.success = false
    }

    override fun componentDidMount() {
        resultInput.focus()
    }

    override fun RBuilder.render() {
        div {
            child(Ticker)
            child(RestTodo)
            if (state.actual != "6") {
                child(Input)
            }

            if (state.success) {
                catPicture()
                return@div
            }

            attrs.classes = setOf("exercise")

            h1 {
                +"Úkol (máš ${state.triesLeft} ${if (state.triesLeft == 1) "pokus" else "pokusy"}):"
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
                    +"${props.a}"
                }
                span { +props.op.value }
                styledSpan {
                    css {
                        color = Color.green
                    }
                    +"${props.b}"
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
                        onChangeFunction = ::onChange
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

    private fun onChange(event: Event) {
        val newValue = (event.target as HTMLInputElement).value
        setState { actual = newValue }
    }

    private fun onSubmit() {
        val expected = props.op.compute(props.a, props.b)
        val actual = state.actual.toIntOrNull()

        if (actual == expected) {
            props.onSuccess()
        } else {
            val triesLeft = state.triesLeft - 1
            if (triesLeft > 0) {
                resultInput.value = cry
                window.setTimeout({ resultInput.value = "" }, 1500)
                setState {
                    this.triesLeft = triesLeft
                }
            } else {
                props.onFailure()
            }
        }
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

val Failure = functionalComponent<RProps> {
    h1 { +cry }
}

sealed class ExerciseState {
    data class Pending(
        val a: Int,
        val b: Int
    ) : ExerciseState()

    object Success : ExerciseState()
    object Failure : ExerciseState()
}

val Main = functionalComponent<RProps> {
    val a = (4..10).random()
    val b = (0..a).random()
    val (result, setResult) = useState(
        ExerciseState.Pending(
            a,
            b
        ) as ExerciseState
    )

    when (result) {
        is ExerciseState.Pending ->
            child(Exercise::class) {
                attrs {
                    this.a = result.a
                    op = Operation.MINUS
                    this.b = result.b
                    onSuccess = {
                        setResult(ExerciseState.Success)
                    }
                    onFailure = {
                        setResult(ExerciseState.Failure)
                    }
                }
            }
        is ExerciseState.Success ->
            child(Success) {
                attrs.onNext = {
                    setResult(ExerciseState.Pending(a, b))
                }
            }
        ExerciseState.Failure -> {
            window.setTimeout({
                setResult(ExerciseState.Pending(a, b))
            }, 3000)
            child(Failure)
        }
    }.let {}
}

fun main() {
    render(document.getElementById("root")) {
        child(Main)
    }
}
