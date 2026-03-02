package com.example.lfk.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lfk.models.*
import com.example.lfk.api.ApiClient
import kotlinx.coroutines.launch

/**
 * ViewModel для аутентификации и управления пользователем
 */
class AuthViewModel : ViewModel() {
    private val apiService = ApiClient.instance

    // Токен авторизации
    private val _authToken = MutableLiveData<String?>()
    val authToken: LiveData<String?> = _authToken

    // Информация о пользователе
    private val _userInfo = MutableLiveData<User?>()
    val userInfo: LiveData<User?> = _userInfo

    // Состояние загрузки
    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    // Ошибка
    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    // Статус аутентификации
    private val _isAuthenticated = MutableLiveData(false)
    val isAuthenticated: LiveData<Boolean> = _isAuthenticated

    /**
     * Вход в систему
     */
    fun login(email: String, password: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null

                val response = apiService.login(LoginRequest(email, password))

                _authToken.value = response.token
                _userInfo.value = response.user
                _isAuthenticated.value = true

                Log.d("AuthViewModel", "Login successful: ${response.user.username}")
                onSuccess()

            } catch (e: Exception) {
                _error.value = "Ошибка входа: ${e.message}"
                Log.e("AuthViewModel", "Login error", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Регистрация нового пользователя
     */
    fun register(
        username: String,
        email: String,
        password: String,
        firstName: String?,
        lastName: String?,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null

                val response = apiService.register(
                    RegisterRequest(
                        username = username,
                        email = email,
                        password = password,
                        first_name = firstName?.takeIf { it.isNotEmpty() },
                        last_name = lastName?.takeIf { it.isNotEmpty() }
                    )
                )

                _authToken.value = response.token
                _userInfo.value = response.user
                _isAuthenticated.value = true

                Log.d("AuthViewModel", "Registration successful: ${response.user.username}")
                onSuccess()

            } catch (e: Exception) {
                _error.value = "Ошибка регистрации: ${e.message}"
                Log.e("AuthViewModel", "Registration error", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Получение профиля пользователя
     */
    fun getProfile() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val token = _authToken.value ?: return@launch

                val profile = apiService.getProfile("Bearer $token")
                _userInfo.value = profile

            } catch (e: Exception) {
                Log.e("AuthViewModel", "Get profile error", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Выход из системы
     */
    fun logout() {
        _authToken.value = null
        _userInfo.value = null
        _isAuthenticated.value = false
    }

    /**
     * Очистка ошибки
     */
    fun clearError() {
        _error.value = null
    }

    /**
     * Проверка, авторизован ли пользователь
     */
    fun isLoggedIn(): Boolean = _authToken.value != null
}