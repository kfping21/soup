package com.zjgsu.soup.game

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.textfield.TextInputEditText
import com.zjgsu.soup.R
import kotlinx.coroutines.launch

class RegisterActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        val usernameEditText = findViewById<TextInputEditText>(R.id.usernameEditText)
        val passwordEditText = findViewById<TextInputEditText>(R.id.passwordEditText)
        val confirmPasswordEditText = findViewById<TextInputEditText>(R.id.confirmPasswordEditText)
        val registerButton = findViewById<Button>(R.id.registerButton)
        val errorTextView = findViewById<TextView>(R.id.errorTextView)

        registerButton.setOnClickListener {
            val username = usernameEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()
            val confirmPassword = confirmPasswordEditText.text.toString().trim()

            // 验证输入
            if (username.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
                errorTextView.text = "请填写所有字段"
                return@setOnClickListener
            }

            if (password != confirmPassword) {
                errorTextView.text = "两次输入的密码不一致"
                return@setOnClickListener
            }

            lifecycleScope.launch {
                val db = AppDatabase.getDatabase(applicationContext)
                val existingUser = db.userDao().getUserByUsername(username)

                if (existingUser != null) {
                    runOnUiThread {
                        errorTextView.text = "用户名已存在"
                    }
                } else {
                    db.userDao().insert(User(username = username, password = password))
                    runOnUiThread {
                        Toast.makeText(
                            this@RegisterActivity,
                            "注册成功",
                            Toast.LENGTH_SHORT
                        ).show()
                        finish() // 返回登录页面
                    }
                }
            }
        }
    }
}