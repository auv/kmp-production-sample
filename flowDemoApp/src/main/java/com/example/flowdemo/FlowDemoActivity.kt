package com.example.flowdemo

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch

class FlowDemoActivity : AppCompatActivity() {

    // 冷流示例：只有被 collect 的时候，flow { } 中的代码才会执行。
    // 每次点击“开始冷流采集”都会重新从 1 开始发射。
    private val coldFlow = flow {
        emit("冷流开始：每次 collect 都会重新执行")
        for (index in 1..5) {
            delay(500)
            emit("冷流发射 -> $index")
        }
        emit("冷流结束")
    }

    // 热流示例：StateFlow 永远持有最新值，新的观察者会立刻收到当前值。
    private val stateFlow = MutableStateFlow(0)

    // 热流示例：SharedFlow 用于一次性事件，订阅者从订阅时刻开始接收。
    private val sharedFlow = MutableSharedFlow<String>(extraBufferCapacity = 1)

    private lateinit var coldOutput: TextView
    private lateinit var hotOutput: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_flow_demo)

        coldOutput = findViewById(R.id.coldOutput)
        hotOutput = findViewById(R.id.hotOutput)

        val coldButton = findViewById<Button>(R.id.buttonCold)
        val stateButton = findViewById<Button>(R.id.buttonState)
        val sharedButton = findViewById<Button>(R.id.buttonShared)
        val clearButton = findViewById<Button>(R.id.buttonClear)

        coldButton.setOnClickListener {
            // 冷流可以多次 collect，每次都会重新执行上面的 flow {} 块。
            lifecycleScope.launch {
                coldFlow.collect { message ->
                    appendCold(message)
                }
            }
        }

        stateButton.setOnClickListener {
            // StateFlow 代表“状态”，更新值后，所有订阅者都会收到最新值。
            stateFlow.value = stateFlow.value + 1
        }

        sharedButton.setOnClickListener {
            // SharedFlow 代表“事件”，每次 emit 都是独立事件。
            sharedFlow.tryEmit("SharedFlow 事件 @${System.currentTimeMillis()}")
        }

        clearButton.setOnClickListener {
            coldOutput.text = ""
            hotOutput.text = ""
        }

        // 在可见生命周期内收集热流。
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    stateFlow.collect { value ->
                        appendHot("StateFlow 当前值 -> $value")
                    }
                }
                launch {
                    sharedFlow.collect { event ->
                        appendHot(event)
                    }
                }
            }
        }
    }

    private fun appendCold(message: String) {
        coldOutput.append("$message\n")
    }

    private fun appendHot(message: String) {
        hotOutput.append("$message\n")
    }
}
