package com.example.recordwithme

//import NotificationScreen
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.navigation.compose.*
import com.example.recordwithme.ui.*
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import androidx.compose.ui.Alignment

class MainActivity : ComponentActivity() {
    private lateinit var googleSignInClient: GoogleSignInClient
    private val viewModel: AuthViewModel by viewModels()
    private val auth: FirebaseAuth by lazy { Firebase.auth }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Google Sign-In Client 초기화
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        // Firebase Auth 상태 리스너 설정
        auth.addAuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            viewModel.setUser(user)
        }

        // 시스템 UI 설정
        enableEdgeToEdge()

        setContent {
            val navController = rememberNavController()
            val user by viewModel.authenticatedUser.collectAsState()
            val context = LocalContext.current

            var drawerOpen by remember { mutableStateOf(false) }
            val currentBackStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = currentBackStackEntry?.destination?.route
            val isAuthScreen = currentRoute == "login" || currentRoute == "signup"

            // 다른 탭으로 이동할 때 GroupMode 전역 상태 초기화
            LaunchedEffect(currentRoute) {
                if (currentRoute != "profile" && GroupModeState.isGroupMode) {
                    GroupModeState.isGroupMode = false
                }
            }

            // 권한 상태 관리
            var hasNotificationPermission by remember {
                mutableStateOf(
                    Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                            ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.POST_NOTIFICATIONS
                            ) == PackageManager.PERMISSION_GRANTED
                )
            }

            // 권한 요청 런처
            val requestPermissionLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { isGranted: Boolean ->
                hasNotificationPermission = isGranted
            }

            // 권한 없으면 요청
            LaunchedEffect(Unit) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                    && ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }

            // 로그인 시 토스트 메시지 표시
            LaunchedEffect(user?.uid) {
                user?.let { firebaseUser ->
                    val db = FirebaseFirestore.getInstance()
                    db.collection("users")
                        .document(firebaseUser.uid)
                        .get()
                        .addOnSuccessListener { document ->
                            if (document.exists()) {
                                val userName = document.getString("name") ?: ""
                                val userId = document.getString("id") ?: ""
                                val displayName =
                                    if (userName.isNotEmpty()) userName else if (userId.isNotEmpty()) "@$userId" else firebaseUser.email ?: "사용자"
                                Toast.makeText(context, "로그인 성공: $displayName", Toast.LENGTH_SHORT)
                                    .show()
                            } else {
                                Toast.makeText(
                                    context,
                                    "로그인 성공: ${firebaseUser.email}",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                        .addOnFailureListener {
                            Toast.makeText(
                                context,
                                "로그인 성공: ${firebaseUser.email}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                }
            }

            // 로그아웃 시 화면 전환
            LaunchedEffect(user) {
                if (user == null && currentRoute != "login") {
                    navController.navigate("login") {
                        popUpTo(0) { inclusive = true }
                    }
                }
            }

            DrawerContainer(
                drawerOpen = drawerOpen,
                onDrawerClose = { drawerOpen = false },
                authViewModel = viewModel
            ) {
                Scaffold(
                    topBar = {
                        if (!isAuthScreen) {
                            TopBar(onMenuClick = { drawerOpen = !drawerOpen })
                        }
                    },
                    bottomBar = {
                        if (!isAuthScreen) {
                            BottomNavigationBar(navController)
                        }
                    }
                ) { innerPadding ->
                    Box(modifier = Modifier.padding(innerPadding)) {
                        NavHost(
                            navController = navController,
                            startDestination = if (user != null) "home" else "login"
                        ) {
                            composable("login") {
                                LoginScreen(
                                    onLoginSuccess = {
                                        navController.navigate("home") {
                                            popUpTo("login") { inclusive = true }
                                        }
                                    },
                                    navController = navController,
                                    onGoogleSignIn = { signIn() }
                                )
                            }
                            composable("signup") {
                                SignUpScreen(navController = navController)
                            }
                            composable("home") { HomeScreen() }
                            composable("profile") { ProfileScreen(navController = navController) }

                            composable("notification") {
                                if (hasNotificationPermission) {
                                    NotificationScreen()
                                } else {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("알림 권한이 필요합니다.")
                                    }
                                }
                            }

                            composable("group") { GroupScreen(navController = navController) }
                        }
                    }
                }
            }
        }
    }

    private fun signIn() {
        val signInIntent = googleSignInClient.signInIntent
        googleSignInLauncher.launch(signInIntent)
    }

    private fun enableEdgeToEdge() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = android.graphics.Color.TRANSPARENT

        val wic = WindowInsetsControllerCompat(window, window.decorView)
        wic.isAppearanceLightStatusBars = true
        wic.isAppearanceLightNavigationBars = true
    }

    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val data = result.data
        try {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            val account = task.getResult(ApiException::class.java)!!
            viewModel.firebaseAuthWithGoogle(account.idToken!!)
        } catch (e: ApiException) {
            Log.w("MainActivity", "Google sign in failed", e)
            viewModel.setUser(null)
        }
    }
}
