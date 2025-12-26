package com.example.fitlifesmarthealthlifestyleapp.ui.auth

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.fitlifesmarthealthlifestyleapp.R
import com.google.android.material.textfield.TextInputEditText

class SignupFragment : Fragment() {

    private lateinit var btnSignIn : TextView
    private lateinit var etFullName : TextInputEditText
    private lateinit var etEmail : TextInputEditText
    private lateinit var etPassword : TextInputEditText
    private lateinit var etConfirmPassword : TextInputEditText
    private lateinit var createAccount: Button
    private lateinit var btnGoogle : Button
    private val viewModel: AuthViewModel by viewModels()
    private lateinit var loadingOverlay : View


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_signup, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        btnSignIn = view.findViewById<TextView>(R.id.tvSignIn)
        etFullName = view.findViewById<TextInputEditText>(R.id.etFullName)
        etEmail = view.findViewById<TextInputEditText>(R.id.etEmail)
        etPassword = view.findViewById<TextInputEditText>(R.id.etPassword)
        etConfirmPassword = view.findViewById<TextInputEditText>(R.id.etConfirmPassword)
        createAccount = view.findViewById<Button>(R.id.btnCreateAccount)
        btnGoogle = view.findViewById<Button>(R.id.btnGoogle)
        loadingOverlay = view.findViewById<View>(R.id.loadingOverlay)


        btnSignIn.setOnClickListener {
            findNavController().popBackStack()
        }

        createAccount.setOnClickListener {
            val fullName = etFullName.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString()
            val confirmPassword = etConfirmPassword.text.toString()

            if (fullName.isEmpty()) {
                etFullName.error = "Full name is required"
            } else if (email.isEmpty()) {
                etEmail.error = "Email is required"
            } else if (password.isEmpty()) {
                etPassword.error = "Password is required"
            } else if (confirmPassword.isEmpty()) {
                etConfirmPassword.error = "Confirm password is required"
            } else if (password != confirmPassword) {
                etConfirmPassword.error = "Passwords do not match"
            } else {
                viewModel.register(fullName, email, password)
            }
        }

        viewModel.authState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is AuthState.Loading -> {
                    loadingOverlay.visibility = View.VISIBLE
                    createAccount.isEnabled = false
                }
                is AuthState.Success -> {
                    loadingOverlay.visibility = View.GONE
                    Toast.makeText(context, "Login successfully", Toast.LENGTH_SHORT).show()

                    findNavController().navigate(R.id.action_signupFragment_to_mainFragment)
                }
                is AuthState.Error -> {
                    loadingOverlay.visibility = View.GONE
                    createAccount.isEnabled = true
                    Toast.makeText(context, "Lỗi: ${state.message}", Toast.LENGTH_LONG).show()
                }
                else -> {}
            }
        }
    }
}