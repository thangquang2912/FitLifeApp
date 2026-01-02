package com.example.fitlifesmarthealthlifestyleapp.ui.auth

import android.app.AlertDialog
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.fitlifesmarthealthlifestyleapp.R
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import androidx.core.widget.addTextChangedListener

class LoginFragment : Fragment() {

    private lateinit var btnSignUp : TextView
    private lateinit var btnSignIn : Button
    private lateinit var tilEmail: TextInputLayout
    private lateinit var tilPassword: TextInputLayout
    private lateinit var etEmail : TextInputEditText
    private lateinit var etPassword : TextInputEditText
    private lateinit var loadingOverlay : View
    private val viewModel: AuthViewModel by viewModels()
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var btnGoogle : Button
    private lateinit var tvForgotPassword : TextView


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_login, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        btnSignUp = view.findViewById<TextView>(R.id.tvSignUp)
        btnSignIn = view.findViewById<Button>(R.id.btnSignIn)
        tilEmail = view.findViewById<TextInputLayout>(R.id.tilEmail)
        tilPassword = view.findViewById<TextInputLayout>(R.id.tilPassword)
        etEmail = view.findViewById<TextInputEditText>(R.id.etEmail)
        etPassword = view.findViewById<TextInputEditText>(R.id.etPassword)
        loadingOverlay = view.findViewById<View>(R.id.loadingOverlay)
        btnGoogle = view.findViewById<Button>(R.id.btnGoogle)
        tvForgotPassword = view.findViewById<TextView>(R.id.tvForgotPassword)

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id)) // Tự sinh từ google-services.json
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(requireActivity(), gso)

        etEmail.addTextChangedListener { tilEmail.error = null }
        etPassword.addTextChangedListener { tilPassword.error = null }

        tvForgotPassword.setOnClickListener {
            showForgotPasswordDialog()
        }

        btnSignUp.setOnClickListener {
            findNavController().navigate(R.id.action_loginFragment_to_signupFragment)
        }

        btnSignIn.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString()

            // Reset lỗi trước khi check
            tilEmail.error = null
            tilPassword.error = null

            var isValid = true

            if (email.isEmpty()) {
                tilEmail.error = "Please enter your email"
                isValid = false
            }
            if (password.isEmpty()) {
                tilPassword.error = "Please enter your password"
                isValid = false
            }

            if (isValid) {
                viewModel.login(email, password)
            }
        }

        btnGoogle.setOnClickListener {
            val intent = googleSignInClient.signInIntent
            googleLauncher.launch(intent)
        }

        viewModel.authState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is AuthState.Loading -> {
                    loadingOverlay.visibility = View.VISIBLE
                    btnSignIn.isEnabled = false // Khóa nút để tránh bấm nhiều lần
                    tilEmail.error = null
                    tilPassword.error = null
                }
                is AuthState.Success -> {
                    loadingOverlay.visibility = View.GONE
                    btnSignIn.isEnabled = true

                    findNavController().navigate(R.id.action_loginFragment_to_mainFragment)
                }
                is AuthState.Error -> {
                    loadingOverlay.visibility = View.GONE
                    btnSignIn.isEnabled = true

                    val msg = state.message
                    // Logic hiển thị lỗi thông minh
                    when {
                        msg.contains("Invalid") -> {
                            tilEmail.error = msg
                            tilPassword.error = msg
                        }
                        msg.contains("Network") -> Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                        else -> Toast.makeText(context, msg, Toast.LENGTH_SHORT).show() // Lỗi khác
                    }
                }
                else -> { /* Idle: Không làm gì */ }
            }
        }

        viewModel.resetPasswordMessage.observe(viewLifecycleOwner) { msg ->
            if (msg != null) {
                if (msg.startsWith("Success")) {
                    // Thành công
                    Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show()
                } else {
                    // Thất bại
                    Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
                }
                viewModel.clearResetMessage()
            }
        }
    }

    private val googleLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            account.idToken?.let { token ->
                viewModel.loginGoogle(token)
            }
        } catch (e: ApiException) {
            Toast.makeText(context, "Google fail: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showForgotPasswordDialog() {
        val builder = AlertDialog.Builder(requireContext())
        val view = layoutInflater.inflate(R.layout.dialog_forgot_password, null) // Bạn cần tạo layout này
        val etEmail = view.findViewById<EditText>(R.id.etResetEmail)

        builder.setView(view)
        builder.setTitle("Reset Password")
        builder.setPositiveButton("Send") { _, _ ->
            val email = etEmail.text.toString().trim()
            viewModel.resetPassword(email)
        }
        builder.setNegativeButton("Cancel", null)
        builder.show()
    }
}