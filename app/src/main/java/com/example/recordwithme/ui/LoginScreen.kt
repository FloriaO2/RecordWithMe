package com.example.recordwithme.ui

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.recordwithme.R
import com.google.firebase.auth.FirebaseAuth

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    navController: NavController,
    onGoogleSignIn: () -> Unit
) {
    var id by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    
    val passwordFocusRequester = remember { FocusRequester() }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 30.dp, start = 32.dp, end = 32.dp, bottom = 32.dp)
        ) {
            // 상단 로고
            Image(
                painter = painterResource(id = R.drawable.logo),
                contentDescription = "앱 로고",
                modifier = Modifier
                    .height(250.dp)
                    .padding(bottom = 10.dp)
            )
            Text(
                "RecordWithMe",
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Cursive,
                fontSize = 30.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = id,
                onValueChange = { id = it },
                label = { Text("아이디") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                singleLine = true,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    imeAction = ImeAction.Next
                ),
                keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                    onNext = {
                        passwordFocusRequester.requestFocus()
                    }
                )
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("비밀번호") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(passwordFocusRequester),
                shape = RoundedCornerShape(20.dp),
                singleLine = true,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    imeAction = ImeAction.Done
                ),
                keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                    onDone = {
                        // 로그인 버튼 클릭과 동일한 로직 실행
                        if (id.isBlank() || password.isBlank()) {
                            Toast.makeText(context, "아이디와 비밀번호를 모두 입력하세요.", Toast.LENGTH_SHORT).show()
                            return@KeyboardActions
                        }
                        
                        // 아이디를 페이크 이메일로 변환
                        val fakeEmail = "$id@recordwith.me"
                        
                        // Firebase Authentication을 사용한 로그인
                        auth.signInWithEmailAndPassword(fakeEmail, password)
                            .addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    Toast.makeText(context, "로그인 성공!", Toast.LENGTH_SHORT).show()
                                    onLoginSuccess()
                                } else {
                                    val errorMessage = when (task.exception) {
                                        is com.google.firebase.auth.FirebaseAuthInvalidUserException -> "존재하지 않는 아이디입니다."
                                        is com.google.firebase.auth.FirebaseAuthInvalidCredentialsException -> "비밀번호가 일치하지 않습니다."
                                        else -> "로그인 실패: ${task.exception?.message}"
                                    }
                                    Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show()
                                }
                            }
                    }
                )
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = {
                    if (id.isBlank() || password.isBlank()) {
                        Toast.makeText(context, "아이디와 비밀번호를 모두 입력하세요.", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    
                    // 아이디를 페이크 이메일로 변환
                    val fakeEmail = "$id@recordwith.me"
                    
                    // Firebase Authentication을 사용한 로그인
                    auth.signInWithEmailAndPassword(fakeEmail, password)
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                Toast.makeText(context, "로그인 성공!", Toast.LENGTH_SHORT).show()
                                onLoginSuccess()
                            } else {
                                val errorMessage = when (task.exception) {
                                    is com.google.firebase.auth.FirebaseAuthInvalidUserException -> "존재하지 않는 아이디입니다."
                                    is com.google.firebase.auth.FirebaseAuthInvalidCredentialsException -> "비밀번호가 일치하지 않습니다."
                                    else -> "로그인 실패: ${task.exception?.message}"
                                }
                                Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show()
                            }
                        }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFFFFC0CB)),
                shape = RoundedCornerShape(20.dp)
            ) {
                Text("기존 계정으로 로그인", fontWeight = FontWeight.Bold, color = Color.White)
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 구글 로그인 버튼 추가
            Button(
                onClick = onGoogleSignIn,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF4285F4)),
                shape = RoundedCornerShape(20.dp)
            ) {
                Text(
                    text = "Google로 로그인",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }

            Text(
                text = "계정이 없으신가요? 회원가입",
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1976D2),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { navController.navigate("signup") }
                    .padding(vertical = 16.dp),
                textAlign = TextAlign.Center
            )
        }
    }
}