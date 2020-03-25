import kotlinx.coroutines.MainScope
import kotlinx.coroutines.await
import kotlinx.coroutines.launch
import kotlinx.css.*
import kotlinx.html.InputType
import kotlinx.html.classes
import kotlinx.html.js.onChangeFunction
import kotlinx.html.js.onKeyDownFunction
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
}

interface ExerciseState : RState {
    var triesLeft: Int
    var actual: String
    var success: Boolean
}

fun RBuilder.catPicture() = img {
    attrs.src = "https://cataas.com/cat?${Random.nextInt(0, 10000000)}"
}

val cry = "\uD83D\uDE22"

val Ticker = functionalComponent<RProps> {
    val (tick, setTick) = useState(0)
    window.setTimeout({
        setTick(tick + 1)
    }, 1000)
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

            console.log(info)
            setTitle(info.title)
        }
    }

    div { +"Todo Title: $title" }
}

val Input = functionalComponent<RProps> {
    val smilies = listOf(
        "😛",  // 0
        "😜",  // 1
        "😝",  // 2
        "🤪"   // 3
    )
    val (idx, setIdx) = useState(0)

    println("HERE")
    window.setTimeout({
        setIdx((idx + 1) % smilies.size)
    }, 500)

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
            placeholder = smilies[idx]
            type = InputType.text
        }
    }
}

class Exercise : RComponent<ExerciseProps, ExerciseState>() {
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
            child(Input)

            if (state.triesLeft == 0) {
                h1 { +cry }
                window.setTimeout({
                    window.location.href = window.location.href
                }, 3000)
                return@div
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
            resultInput.value = cry
            window.setTimeout({ resultInput.value = "" }, 1500)
            setState {
                this.triesLeft = triesLeft - 1
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

fun main() {
    fun onExerciseSuccess() {
        render(document.getElementById("root")) {
            catPicture()
            br { }
            br { }

            a {
                attrs.href = window.location.href
                +"Chci další úkol.."
            }
        }
    }

    val a = (0..10).random()
    val b = (0..a).random()

    render(document.getElementById("root")) {
        child(Exercise::class) {
            attrs {
                this.a = a
                op = Operation.MINUS
                this.b = b
                onSuccess = ::onExerciseSuccess
            }
        }
    }
}
