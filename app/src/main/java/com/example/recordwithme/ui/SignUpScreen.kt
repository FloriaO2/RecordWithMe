package com.example.recordwithme.ui

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun SignUpScreen(navController: NavController) {
    val context = LocalContext.current
    var step by remember { mutableStateOf(1) }
    var name by remember { mutableStateOf("") }
    var id by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordCheck by remember { mutableStateOf("") }
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    
    val idFocusRequester = remember { FocusRequester() }
    val passwordFocusRequester = remember { FocusRequester() }
    val passwordCheckFocusRequester = remember { FocusRequester() }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 20.dp, start = 32.dp, end = 32.dp, bottom = 32.dp)
        ) {
            // 상단 뒤로가기 버튼과 타이틀
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                IconButton(onClick = {
                    when (step) {
                        1 -> navController.popBackStack() // 로그인 화면으로 이동
                        2 -> step = 1
                        3 -> step = 2
                    }
                }) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "뒤로가기")
                }
                Spacer(modifier = Modifier.width(8.dp))
                val title = when (step) {
                    1 -> "사용자 이름 만들기"
                    2 -> "아이디 만들기"
                    3 -> "비밀번호 만들기"
                    else -> "회원가입"
                }
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                    modifier = Modifier.padding(vertical = 24.dp)
                )
            }
            Spacer(modifier = Modifier.height(32.dp))
            
            // 단계별 자동 포커스 설정
            LaunchedEffect(step) {
                when (step) {
                    2 -> idFocusRequester.requestFocus()
                    3 -> passwordFocusRequester.requestFocus()
                }
            }
            
            when (step) {
                1 -> {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("사용자 이름") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(15.dp),
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                            onNext = {
                                if (name.isNotBlank()) step = 2
                                else Toast.makeText(context, "이름을 입력하세요.", Toast.LENGTH_SHORT).show()
                            }
                        )
                    )
                    Spacer(modifier = Modifier.height(32.dp))
                    Button(
                        onClick = {
                            if (name.isNotBlank()) step = 2
                            else Toast.makeText(context, "이름을 입력하세요.", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFFFFC0CB)),
                        shape = RoundedCornerShape(15.dp)
                    ) {
                        Text("다음", fontWeight = FontWeight.Bold)
                    }
                }
                2 -> {
                    OutlinedTextField(
                        value = id,
                        onValueChange = { id = it },
                        label = { Text("아이디") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(idFocusRequester),
                        shape = RoundedCornerShape(15.dp),
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                            onNext = {
                                if (id.isBlank()) {
                                    Toast.makeText(context, "아이디를 입력하세요.", Toast.LENGTH_SHORT).show()
                                } else {
                                    // 아이디 중복 체크
                                    db.collection("users")
                                        .whereEqualTo("id", id)
                                        .get()
                                        .addOnSuccessListener { documents ->
                                            if (documents.isEmpty) {
                                                step = 3
                                            } else {
                                                Toast.makeText(context, "이미 사용 중인 아이디입니다.", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                        .addOnFailureListener {
                                            Toast.makeText(context, "아이디 확인 중 오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
                                        }
                                }
                            }
                        )
                    )
                    Spacer(modifier = Modifier.height(32.dp))
                    Button(
                        onClick = {
                            if (id.isBlank()) {
                                Toast.makeText(context, "아이디를 입력하세요.", Toast.LENGTH_SHORT).show()
                            } else {
                                // 아이디 중복 체크
                                db.collection("users")
                                    .whereEqualTo("id", id)
                                    .get()
                                    .addOnSuccessListener { documents ->
                                        if (documents.isEmpty) {
                                            step = 3
                                        } else {
                                            Toast.makeText(context, "이미 사용 중인 아이디입니다.", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                    .addOnFailureListener {
                                        Toast.makeText(context, "아이디 확인 중 오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
                                    }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFFFFC0CB)),
                        shape = RoundedCornerShape(15.dp)
                    ) {
                        Text("다음", fontWeight = FontWeight.Bold)
                    }
                }
                3 -> {
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("비밀번호") },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(passwordFocusRequester),
                        shape = RoundedCornerShape(15.dp),
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                            onNext = {
                                passwordCheckFocusRequester.requestFocus()
                            }
                        )
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = passwordCheck,
                        onValueChange = { passwordCheck = it },
                        label = { Text("비밀번호 확인") },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(passwordCheckFocusRequester),
                        shape = RoundedCornerShape(15.dp),
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                            onDone = {
                                // 회원가입 버튼 클릭과 동일한 로직 실행
                                if (password.isBlank() || passwordCheck.isBlank()) {
                                    Toast.makeText(context, "비밀번호를 모두 입력하세요.", Toast.LENGTH_SHORT).show()
                                } else if (password != passwordCheck) {
                                    Toast.makeText(context, "비밀번호가 일치하지 않습니다.", Toast.LENGTH_SHORT).show()
                                } else if (password.length < 6) {
                                    Toast.makeText(context, "비밀번호는 6자 이상이어야 합니다.", Toast.LENGTH_SHORT).show()
                                } else {
                                    // 아이디를 페이크 이메일로 변환
                                    val fakeEmail = "$id@recordwith.me"
                                    
                                    // Firebase Authentication을 사용한 회원가입
                                    auth.createUserWithEmailAndPassword(fakeEmail, password)
                                        .addOnCompleteListener { task ->
                                            if (task.isSuccessful) {
                                                // 회원가입 성공 시 Firestore에 사용자 정보 저장
                                                val user = auth.currentUser
                                                val userInfo = hashMapOf(
                                                    "name" to name,
                                                    "id" to id,
                                                    "email" to fakeEmail,
                                                    "uid" to (user?.uid ?: id),
                                                )
                                                
                                                // 사용자 문서 ID를 id값으로 설정
                                                db.collection("users")
                                                    .document(id)
                                                    .set(userInfo)
                                                    .addOnSuccessListener {
                                                        Toast.makeText(context, "회원가입 성공!", Toast.LENGTH_SHORT).show()
                                                        navController.popBackStack("login", inclusive = false)
                                                    }
                                                    .addOnFailureListener {
                                                        Toast.makeText(context, "사용자 정보 저장 실패", Toast.LENGTH_SHORT).show()
                                                    }
                                            } else {
                                                val errorMessage = when (task.exception) {
                                                    is com.google.firebase.auth.FirebaseAuthWeakPasswordException -> "비밀번호가 너무 약합니다."
                                                    is com.google.firebase.auth.FirebaseAuthInvalidCredentialsException -> "올바르지 않은 아이디 형식입니다."
                                                    else -> "회원가입 실패: ${task.exception?.message}"
                                                }
                                                Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                }
                            }
                        )
                    )
                    Spacer(modifier = Modifier.height(32.dp))
                    Button(
                        onClick = {
                            if (password.isBlank() || passwordCheck.isBlank()) {
                                Toast.makeText(context, "비밀번호를 모두 입력하세요.", Toast.LENGTH_SHORT).show()
                            } else if (password != passwordCheck) {
                                Toast.makeText(context, "비밀번호가 일치하지 않습니다.", Toast.LENGTH_SHORT).show()
                            } else if (password.length < 6) {
                                Toast.makeText(context, "비밀번호는 6자 이상이어야 합니다.", Toast.LENGTH_SHORT).show()
                            } else {
                                // 아이디를 페이크 이메일로 변환
                                val fakeEmail = "$id@recordwith.me"
                                
                                // Firebase Authentication을 사용한 회원가입
                                auth.createUserWithEmailAndPassword(fakeEmail, password)
                                    .addOnCompleteListener { task ->
                                        if (task.isSuccessful) {
                                            // 회원가입 성공 시 Firestore에 사용자 정보 저장
                                            val user = auth.currentUser
                                            val userInfo = hashMapOf(
                                                "name" to name,
                                                "id" to id,
                                                "email" to fakeEmail,
                                                "uid" to (user?.uid ?: id),
                                            )
                                            
                                            // 사용자 문서 ID를 id값으로 설정
                                            db.collection("users")
                                                .document(id)
                                                .set(userInfo)
                                                .addOnSuccessListener {
                                                    Toast.makeText(context, "회원가입 성공!", Toast.LENGTH_SHORT).show()
                                                    navController.popBackStack("login", inclusive = false)
                                                }
                                                .addOnFailureListener {
                                                    Toast.makeText(context, "사용자 정보 저장 실패", Toast.LENGTH_SHORT).show()
                                                }
                                        } else {
                                            val errorMessage = when (task.exception) {
                                                is com.google.firebase.auth.FirebaseAuthWeakPasswordException -> "비밀번호가 너무 약합니다."
                                                is com.google.firebase.auth.FirebaseAuthInvalidCredentialsException -> "올바르지 않은 아이디 형식입니다."
                                                else -> "회원가입 실패: ${task.exception?.message}"
                                            }
                                            Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show()
                                        }
                                    }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFFFFC0CB)),
                        shape = RoundedCornerShape(15.dp)
                    ) {
                        Text("회원가입", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}