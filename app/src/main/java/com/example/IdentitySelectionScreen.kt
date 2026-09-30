package com.example

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.viewmodel.StudyViewModel
import kotlinx.coroutines.launch
import java.util.Locale

@Composable
fun IdentityContent(
    currentX: Float,
    currentY: Float,
    viewModel: StudyViewModel,
    onLoginSuccess: () -> Unit
) {
    val context = LocalContext.current
    val strings = LocalAppStrings.current
    val isAuthLoading by viewModel.isAuthLoading.observeAsState(false)
    var showEmailForm by remember { mutableStateOf(false) }
    var isSignUp by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp, vertical = 36.dp)
                .graphicsLayer {
                    translationX = currentX * -15f
                    translationY = currentY * -15f
                },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            HeroCharacter()

            Text(
                text = "Kiki Hihi",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black,
                color = Color.White
            )

            Text(
                text = "Gia Sư Tiếng Anh AI Cá Nhân Hóa",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF51FAC1),
                modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
            )

            // Card xác thực chính
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(32.dp))
                    .background(Color(0x33141424))
                    .border(
                        width = 1.dp,
                        brush = Brush.linearGradient(
                            colors = listOf(Color(0x6651FAC1), Color(0x1AFFFFFF), Color(0x33FFD166))
                        ),
                        shape = RoundedCornerShape(32.dp)
                    )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 22.dp, vertical = 26.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Chào mừng bạn trở lại!",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = "Đăng nhập để đồng bộ tiến trình học và mở khóa huy hiệu",
                        color = Color(0x99FFFFFF),
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 4.dp, bottom = 22.dp)
                    )

                    // 1. Nút "Tiếp tục với Google"
                    GoogleSignInButton(
                        isLoading = isAuthLoading,
                        onClick = {
                            viewModel.signInWithGoogle(
                                activityContext = context,
                                onSuccess = onLoginSuccess,
                                onError = { message ->
                                    Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                                }
                            )
                        }
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // 2. Nút "Khám phá ngay (Chế độ Khách)"
                    GuestSignInButton(
                        isLoading = isAuthLoading,
                        onClick = {
                            viewModel.signInAsGuest(
                                onSuccess = onLoginSuccess,
                                onError = { message ->
                                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Hoặc Divider
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        HorizontalDivider(
                            modifier = Modifier.weight(1f),
                            thickness = 0.8.dp,
                            color = Color(0x26FFFFFF)
                        )
                        Text(
                            text = "HOẶC",
                            color = Color(0x66FFFFFF),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )
                        HorizontalDivider(
                            modifier = Modifier.weight(1f),
                            thickness = 0.8.dp,
                            color = Color(0x26FFFFFF)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Toggle form Email
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { showEmailForm = !showEmailForm }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (showEmailForm) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            tint = Color(0xFF51FAC1),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (showEmailForm) "Ẩn đăng nhập Email" else "Đăng nhập bằng Email & Mật khẩu",
                            color = Color(0xFF51FAC1),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    // Form Email mở rộng
                    AnimatedVisibility(
                        visible = showEmailForm,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 14.dp)
                        ) {
                            AuthForm(
                                isSignUp = isSignUp,
                                onToggleAuth = { isSignUp = !isSignUp },
                                onSubmit = { name, email, pass ->
                                    val normalizedEmail = email.trim().lowercase(Locale.ROOT)
                                    if (!android.util.Patterns.EMAIL_ADDRESS.matcher(normalizedEmail).matches()) {
                                        Toast.makeText(context, strings.emailMustEndWithGmail, Toast.LENGTH_SHORT).show()
                                        return@AuthForm
                                    }
                                    if (isSignUp) {
                                        if (validatePassword(pass)) {
                                            coroutineScope.launch {
                                                val res = viewModel.repository.registerUser(name.trim(), normalizedEmail, pass)
                                                if (res == -2L) {
                                                    Toast.makeText(context, strings.emailExists, Toast.LENGTH_SHORT).show()
                                                } else {
                                                    Toast.makeText(context, strings.regSuccess, Toast.LENGTH_SHORT).show()
                                                    isSignUp = false
                                                }
                                            }
                                        } else {
                                            Toast.makeText(context, strings.passwordReq, Toast.LENGTH_LONG).show()
                                        }
                                    } else {
                                        coroutineScope.launch {
                                            val user = viewModel.repository.login(normalizedEmail, pass)
                                            if (user != null) {
                                                viewModel.loadUserData(normalizedEmail)
                                                onLoginSuccess()
                                            } else {
                                                Toast.makeText(context, strings.incorrectLogin, Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = strings.kikiQuote,
                color = Color(0x66FFFFFF),
                fontStyle = FontStyle.Italic,
                textAlign = TextAlign.Center,
                fontSize = 12.sp,
                modifier = Modifier.padding(horizontal = 24.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = Color(0x5551FAC1),
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Bảo mật tài khoản & đồng bộ đám mây",
                    color = Color(0x66FFFFFF),
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
fun GoogleSignInButton(
    isLoading: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .clickable(enabled = !isLoading, onClick = onClick)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(22.dp),
                color = Color(0xFF4285F4),
                strokeWidth = 2.5.dp
            )
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                GoogleLogo(modifier = Modifier.size(22.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Tiếp tục với Google",
                    color = Color(0xFF1F1F1F),
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }
        }
    }
}

@Composable
fun GuestSignInButton(
    isLoading: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(Color(0x2651FAC1), Color(0x1A14142B), Color(0x2651FAC1))
                )
            )
            .border(
                width = 1.2.dp,
                brush = Brush.horizontalGradient(
                    listOf(Color(0xFF51FAC1), Color(0x6600F5D4), Color(0xFF51FAC1))
                ),
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(enabled = !isLoading, onClick = onClick)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Explore,
                contentDescription = null,
                tint = Color(0xFF51FAC1),
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "Khám phá ngay (Chế độ Khách)",
                color = Color(0xFF51FAC1),
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
fun GoogleLogo(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val strokeWidth = w * 0.22f

        // Đỏ: Cung trên
        drawArc(
            color = Color(0xFFEA4335),
            startAngle = 195f,
            sweepAngle = 110f,
            useCenter = false,
            topLeft = Offset(strokeWidth / 2, strokeWidth / 2),
            size = Size(w - strokeWidth, h - strokeWidth),
            style = Stroke(width = strokeWidth)
        )
        // Vàng: Cung trái
        drawArc(
            color = Color(0xFFFBBC05),
            startAngle = 135f,
            sweepAngle = 70f,
            useCenter = false,
            topLeft = Offset(strokeWidth / 2, strokeWidth / 2),
            size = Size(w - strokeWidth, h - strokeWidth),
            style = Stroke(width = strokeWidth)
        )
        // Xanh lá: Cung dưới
        drawArc(
            color = Color(0xFF34A853),
            startAngle = 45f,
            sweepAngle = 95f,
            useCenter = false,
            topLeft = Offset(strokeWidth / 2, strokeWidth / 2),
            size = Size(w - strokeWidth, h - strokeWidth),
            style = Stroke(width = strokeWidth)
        )
        // Xanh lam: Cung phải
        drawArc(
            color = Color(0xFF4285F4),
            startAngle = -45f,
            sweepAngle = 95f,
            useCenter = false,
            topLeft = Offset(strokeWidth / 2, strokeWidth / 2),
            size = Size(w - strokeWidth, h - strokeWidth),
            style = Stroke(width = strokeWidth)
        )
        // Thanh ngang của chữ G
        drawRect(
            color = Color(0xFF4285F4),
            topLeft = Offset(w * 0.45f, h * 0.40f),
            size = Size(w * 0.50f, strokeWidth * 0.95f)
        )
    }
}

@Composable
fun AuthForm(
    isSignUp: Boolean,
    onToggleAuth: () -> Unit,
    onSubmit: (String, String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    val strings = LocalAppStrings.current
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = if (isSignUp) strings.createAccount else strings.welcomeBack,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (isSignUp) {
            CosmicTextField(value = name, onValueChange = { name = it }, label = strings.fullName, icon = Icons.Default.Person)
            Spacer(modifier = Modifier.height(10.dp))
        }

        CosmicTextField(value = email, onValueChange = { email = it }, label = strings.emailAddress, icon = Icons.Default.Email)
        Spacer(modifier = Modifier.height(10.dp))

        CosmicTextField(
            value = password,
            onValueChange = { password = it },
            label = strings.passwordLabel,
            icon = Icons.Default.Lock,
            isPassword = true,
            passwordVisible = passwordVisible,
            onPasswordVisibleChange = { passwordVisible = it }
        )

        Spacer(modifier = Modifier.height(18.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Brush.linearGradient(listOf(Color(0xFFFF9E00), Color(0xFF00F5D4))))
                .clickable { onSubmit(name, email, password) },
            contentAlignment = Alignment.Center
        ) {
            Text(
                if (isSignUp) strings.signUp else strings.login,
                color = Color(0xFF002116),
                fontWeight = FontWeight.ExtraBold,
                fontSize = 16.sp
            )
        }

        TextButton(onClick = onToggleAuth) {
            Text(
                if (isSignUp) strings.alreadyHaveAccount else strings.dontHaveAccount,
                color = Color(0xFF51FAC1),
                fontSize = 13.sp
            )
        }
    }
}

@Composable
fun CosmicTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isPassword: Boolean = false,
    passwordVisible: Boolean = false,
    onPasswordVisibleChange: (Boolean) -> Unit = {}
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label, color = Color.White.copy(alpha = 0.6f), fontSize = 13.sp) },
        leadingIcon = { Icon(icon, contentDescription = null, tint = Color(0xFF51FAC1), modifier = Modifier.size(20.dp)) },
        trailingIcon = if (isPassword) {
            {
                IconButton(onClick = { onPasswordVisibleChange(!passwordVisible) }) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.6f)
                    )
                }
            }
        } else null,
        visualTransformation = if (isPassword && !passwordVisible) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = if (isPassword) KeyboardOptions(keyboardType = KeyboardType.Password) else KeyboardOptions.Default,
        shape = RoundedCornerShape(14.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            focusedBorderColor = Color(0xFF51FAC1),
            unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
            cursorColor = Color(0xFF51FAC1)
        )
    )
}

fun validatePassword(password: String): Boolean {
    val pattern = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z]).{8,}$"
    return password.matches(pattern.toRegex())
}

@Composable
fun HeroCharacter() {
    Box(
        modifier = Modifier
            .padding(bottom = 16.dp)
            .size(150.dp),
        contentAlignment = Alignment.Center
    ) {
        val infiniteTransition = rememberInfiniteTransition(label = "hero")
        val pulseGlow by infiniteTransition.animateFloat(
            initialValue = 1f, targetValue = 1.05f,
            animationSpec = infiniteRepeatable(tween(2000, easing = LinearEasing), RepeatMode.Reverse),
            label = "pulse"
        )
        val floatAnim by infiniteTransition.animateFloat(
            initialValue = -8f, targetValue = 8f,
            animationSpec = infiniteRepeatable(tween(3000, easing = LinearEasing), RepeatMode.Reverse),
            label = "float"
        )
        val rotateRing by infiniteTransition.animateFloat(
            initialValue = 0f, targetValue = 360f,
            animationSpec = infiniteRepeatable(tween(6000, easing = LinearEasing)),
            label = "rotate"
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .scale(1.4f * pulseGlow)
                .blur(26.dp)
                .background(Color(0x3354FDC4), CircleShape)
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .offset(y = floatAnim.dp)
                .drawBehind {
                    withTransform({ rotate(rotateRing) }) {
                        drawCircle(
                            brush = Brush.sweepGradient(listOf(Color(0xFF00F5D4), Color(0xFF785A00), Color(0xFFFF9E00), Color(0xFF00F5D4))),
                            style = Stroke(width = 3.5.dp.toPx())
                        )
                    }
                }
                .padding(4.dp)
                .clip(CircleShape)
        ) {
            AsyncImage(
                model = R.drawable.kiki_hero_auth,
                contentDescription = "Kiki Avatar",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
    }
}
