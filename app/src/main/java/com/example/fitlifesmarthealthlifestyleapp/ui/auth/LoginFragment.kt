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
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import com.example.fitlifesmarthealthlifestyleapp.R
import com.google.android.material.textfield.TextInputEditText

class LoginFragment : Fragment() {

    private lateinit var btnSignUp : TextView
    private lateinit var btnSignIn : Button
    private lateinit var etEmail : TextInputEditText
    private lateinit var etPassword : TextInputEditText
    private lateinit var loadingOverlay : View
    private val viewModel: AuthViewModel by viewModels()


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
        etEmail = view.findViewById<TextInputEditText>(R.id.etEmail)
        etPassword = view.findViewById<TextInputEditText>(R.id.etPassword)
        loadingOverlay = view.findViewById<View>(R.id.loadingOverlay)


        btnSignUp.setOnClickListener {
            findNavController().navigate(R.id.action_loginFragment_to_signupFragment)
        }


        btnSignIn.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString()

            if (email.isNotEmpty() && password.isNotEmpty()) {
                viewModel.login(email, password)
            } else {
                Toast.makeText(requireContext(), "Please fill in all fields", Toast.LENGTH_SHORT).show()
            }
        }

        viewModel.authState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is AuthState.Loading -> {
                    loadingOverlay.visibility = View.VISIBLE
                    btnSignIn.isEnabled = false // Khóa nút để tránh bấm nhiều lần
                }
                is AuthState.Success -> {
                    loadingOverlay.visibility = View.GONE
                    btnSignIn.isEnabled = true
                    Toast.makeText(context, "Đăng nhập thành công!", Toast.LENGTH_SHORT).show()

                    findNavController().navigate(R.id.action_loginFragment_to_mainFragment)
                }
                is AuthState.Error -> {
                    loadingOverlay.visibility = View.GONE
                    btnSignIn.isEnabled = true
                    Toast.makeText(context, state.message, Toast.LENGTH_SHORT).show()
                }
                else -> { /* Idle: Không làm gì */ }
            }
        }
    }
}