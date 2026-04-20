package com.example.lf.viewmodel

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lf.api.ApiClient
import com.example.lf.api.LoginRequest
import com.example.lf.api.RegisterRequest
import com.example.lf.api.UserResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AuthState(
    val isLoading: Boolean = false,
    val isAuthenticated: Boolean = false,
    val user: UserResponse? = null,
    val error: String? = null
)

class AuthViewModel(private val context: Context) : ViewModel() {

    private val prefs: SharedPreferences = context.getSharedPreferences("lf_prefs", Context.MODE_PRIVATE)

    private val _authState = MutableStateFlow(AuthState())
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val _authToken = MutableStateFlow<String?>(null)
    val authToken: StateFlow<String?> = _authToken.asStateFlow()

    private val _userInfo = MutableStateFlow<UserResponse?>(null)
    val userInfo: StateFlow<UserResponse?> = _userInfo.asStateFlow()

    // Колбэк для загрузки данных после входа
    var onLoginSuccess: (() -> Unit)? = null

    init {
        val savedToken = prefs.getString("auth_token", null)
        if (savedToken != null) {
            _authToken.value = savedToken
            loadUserProfile(savedToken)
        }
    }

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _authState.value = _authState.value.copy(isLoading = true, error = null)

            try {
                val request = LoginRequest(email, password)
                val response = ApiClient.apiService.login(request)

                if (response.isSuccessful && response.body() != null) {
                    val authResponse = response.body()!!
                    _authToken.value = authResponse.token
                    _userInfo.value = authResponse.user
                    _authState.value = AuthState(
                        isLoading = false,
                        isAuthenticated = true,
                        user = authResponse.user,
                        error = null
                    )
                    prefs.edit().putString("auth_token", authResponse.token).apply()

                    // Вызываем колбэк после успешного входа
                    onLoginSuccess?.invoke()
                } else {
                    val errorMsg = response.errorBody()?.string() ?: "Ошибка входа"
                    _authState.value = AuthState(
                        isLoading = false,
                        isAuthenticated = false,
                        error = errorMsg
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _authState.value = AuthState(
                    isLoading = false,
                    isAuthenticated = false,
                    error = e.message ?: "Ошибка соединения"
                )
            }
        }
    }

    fun register(username: String, email: String, password: String, firstName: String? = null, lastName: String? = null) {
        viewModelScope.launch {
            _authState.value = _authState.value.copy(isLoading = true, error = null)

            try {
                val request = RegisterRequest(
                    username = username,
                    email = email,
                    password = password,
                    first_name = firstName,
                    last_name = lastName
                )
                val response = ApiClient.apiService.register(request)

                if (response.isSuccessful && response.body() != null) {
                    val authResponse = response.body()!!
                    _authToken.value = authResponse.token
                    _userInfo.value = authResponse.user
                    _authState.value = AuthState(
                        isLoading = false,
                        isAuthenticated = true,
                        user = authResponse.user,
                        error = null
                    )
                    prefs.edit().putString("auth_token", authResponse.token).apply()

                    // Вызываем колбэк после успешной регистрации
                    onLoginSuccess?.invoke()
                } else {
                    val errorMsg = response.errorBody()?.string() ?: "Ошибка регистрации"
                    _authState.value = AuthState(
                        isLoading = false,
                        isAuthenticated = false,
                        error = errorMsg
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _authState.value = AuthState(
                    isLoading = false,
                    isAuthenticated = false,
                    error = e.message ?: "Ошибка соединения"
                )
            }
        }
    }

    private fun loadUserProfile(token: String) {
        viewModelScope.launch {
            try {
                val response = ApiClient.apiService.getProfile("Bearer $token")
                if (response.isSuccessful && response.body() != null) {
                    _userInfo.value = response.body()!!
                    _authState.value = _authState.value.copy(
                        isAuthenticated = true,
                        user = response.body()!!
                    )
                    // Если токен валидный, загружаем данные
                    onLoginSuccess?.invoke()
                } else {
                    logout()
                }
            } catch (e: Exception) {
                logout()
            }
        }
    }

    fun logout() {
        _authState.value = AuthState()
        _authToken.value = null
        _userInfo.value = null
        prefs.edit().remove("auth_token").apply()
    }

    fun clearError() {
        _authState.value = _authState.value.copy(error = null)
    }

    fun getDisplayName(): String {
        val user = _userInfo.value
        val firstName = user?.getFirstName()
        val username = user?.username ?: "Пользователь"
        return firstName ?: username
    }
}