
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

@Composable
fun LoginScreen(
    navController: NavController,
    onGoogleSignIn: () -> Unit,
    onLoginSuccess: () -> Unit
) {
    var id by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // (기존 아이디, 비밀번호 입력필드, 로그인 버튼 등 필요하면 추가)

        Spacer(modifier = Modifier.height(16.dp))

        // 회원가입 텍스트
        Text(
            text = "계정이 없으신가요? 회원가입",
            modifier = Modifier
                .clickable { navController.navigate("signup") }
                .padding(8.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        // 구글 로그인 버튼 (회원가입 텍스트 바로 아래)
        Button(
            onClick = onGoogleSignIn,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "Google로 로그인")
        }
    }
}