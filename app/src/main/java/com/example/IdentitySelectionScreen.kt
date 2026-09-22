package com.example

import android.widget.Toast
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
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
    var isSignUp by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val strings = LocalAppStrings.current
    val coroutineScope = rememberCoroutineScope()

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .graphicsLayer {
                    translationX = currentX * -20f
                    translationY = currentY * -20f
                },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            HeroCharacter()

            Text(
                text = "Kiki Hihi",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(32.dp))
                    .background(Color(0x2614141E))
                    .border(
                        width = 1.dp,
                        brush = Brush.linearGradient(
                            colors = listOf(Color(0x66FFFFFF), Color(0x00FFFFFF), Color(0x1AFFFFFF))
                        ),
                        shape = RoundedCornerShape(32.dp)
                    )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    AuthForm(
                        isSignUp = isSignUp,
                        onToggleAuth = { isSignUp = !isSignUp },
                        onSubmit = { name, email, pass ->
                            val normalizedEmail = email.trim().lowercase(Locale.ROOT)
                            if (!normalizedEmail.endsWith("@gmail.com")) {
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
                                        viewModel.onLoginSuccess(normalizedEmail)
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

            Spacer(modifier = Modifier.height(32.dp))
            val strings = LocalAppStrings.current
            Text(
                strings.kikiQuote,
                color = Color(0x66FFFFFF),
                fontStyle = FontStyle.Italic,
                textAlign = TextAlign.Center,
                fontSize = 12.sp,
                modifier = Modifier.padding(horizontal = 32.dp)
            )
        }
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
            fontSize = 22.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(24.dp))

        if (isSignUp) {
            CosmicTextField(value = name, onValueChange = { name = it }, label = strings.fullName, icon = Icons.Default.Person)
            Spacer(modifier = Modifier.height(12.dp))
        }

        CosmicTextField(value = email, onValueChange = { email = it }, label = strings.emailAddress, icon = Icons.Default.Email)
        Spacer(modifier = Modifier.height(12.dp))
        
        CosmicTextField(
            value = password,
            onValueChange = { password = it },
            label = strings.passwordLabel,
            icon = Icons.Default.Lock,
            isPassword = true,
            passwordVisible = passwordVisible,
            onPasswordVisibleChange = { passwordVisible = it }
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Brush.linearGradient(listOf(Color(0xFFFF9E00), Color(0xFF00F5D4))))
                .clickable { onSubmit(name, email, password) },
            contentAlignment = Alignment.Center
        ) {
            Text(if (isSignUp) strings.signUp else strings.login, color = Color(0xFF002116), fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
        }

        TextButton(onClick = onToggleAuth) {
            Text(if (isSignUp) strings.alreadyHaveAccount else strings.dontHaveAccount, color = Color(0xFF51FAC1))
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
        label = { Text(label, color = Color.White.copy(alpha = 0.6f)) },
        leadingIcon = { Icon(icon, contentDescription = null, tint = Color(0xFF51FAC1)) },
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
        shape = RoundedCornerShape(16.dp),
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
            .size(160.dp),
        contentAlignment = Alignment.Center
    ) {
        val infiniteTransition = rememberInfiniteTransition(label = "hero")
        val pulseGlow by infiniteTransition.animateFloat(
            initialValue = 1f, targetValue = 1.05f,
            animationSpec = infiniteRepeatable(tween(2000, easing = LinearEasing), RepeatMode.Reverse),
            label = "pulse"
        )
        val floatAnim by infiniteTransition.animateFloat(
            initialValue = -10f, targetValue = 10f,
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
                .scale(1.5f * pulseGlow)
                .blur(30.dp)
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
                            style = Stroke(width = 4.dp.toPx())
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
