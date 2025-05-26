package com.zjgsu.soup

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Button
import android.widget.CheckBox
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.textfield.TextInputEditText
import java.util.Random

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // 生成并显示随机版本号
        val versionTextView = findViewById<TextView>(R.id.versionTextView)
        versionTextView.text = "v${generateRandomVersion()}"

        // 获取UI组件
        val usernameEditText = findViewById<TextInputEditText>(R.id.usernameEditText)
        val passwordEditText = findViewById<TextInputEditText>(R.id.passwordEditText)
        val agreeCheckBox = findViewById<CheckBox>(R.id.agreeCheckBox)
        val loginButton = findViewById<Button>(R.id.loginButton)
        val errorTextView = findViewById<TextView>(R.id.errorTextView)

        // 定义有效用户
        val validUsers = mapOf(
            "panjiawei" to "2312190633",
            "pingkaifei" to "2312190616"
        )

        // 文本变化监听器
        val textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                updateLoginButtonState(
                    usernameEditText.text.toString(),
                    passwordEditText.text.toString(),
                    agreeCheckBox.isChecked,
                    loginButton
                )
            }
        }

        // 为输入框添加监听
        usernameEditText.addTextChangedListener(textWatcher)
        passwordEditText.addTextChangedListener(textWatcher)

        // 勾选框状态变化监听
        agreeCheckBox.setOnCheckedChangeListener { _, isChecked ->
            updateLoginButtonState(
                usernameEditText.text.toString(),
                passwordEditText.text.toString(),
                isChecked,
                loginButton
            )
        }

        // 设置登录按钮点击事件
        loginButton.setOnClickListener {
            val username = usernameEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()

            if (username.isEmpty() || password.isEmpty()) {
                errorTextView.text = "用户名和密码不能为空"
                return@setOnClickListener
            }

            if (!agreeCheckBox.isChecked) {
                errorTextView.text = "请先同意用户协议和隐私政策"
                return@setOnClickListener
            }

            if (validUsers[username] == password) {
                errorTextView.text = ""
                Toast.makeText(this, "登录成功，欢迎 $username", Toast.LENGTH_SHORT).show()
                val intent = Intent(this, LoadingActivity::class.java)
                startActivity(intent)
                finish()  // 这行很重要，关闭当前Activity
            }else {
                errorTextView.text = "用户名或密码错误"
            }
        }
    }

    private fun generateRandomVersion(): String {
        val random = Random()
        return "${random.nextInt(10)}.${random.nextInt(10)}.${random.nextInt(100)}"
    }

    private fun updateLoginButtonState(
        username: String,
        password: String,
        isAgreed: Boolean,
        loginButton: Button
    ) {
        val isEnabled = username.isNotEmpty() &&
                password.isNotEmpty() &&
                isAgreed

        loginButton.isEnabled = isEnabled
        loginButton.alpha = if (isEnabled) 1f else 0.5f
    }
}