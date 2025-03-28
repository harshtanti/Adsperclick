package com.adsperclick.media.views.login.fragments

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.adsperclick.media.R
import com.adsperclick.media.applicationCommonView.TokenManager
import com.adsperclick.media.data.dataModels.NetworkResult
import com.adsperclick.media.databinding.FragmentLoginBinding
import com.adsperclick.media.utils.gone
import com.adsperclick.media.utils.visible
import com.adsperclick.media.views.homeActivity.HomeActivity
import com.adsperclick.media.views.login.MainActivity
import com.adsperclick.media.views.login.viewModels.AuthViewModel
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class LoginFragment : Fragment() {

    lateinit var binding : FragmentLoginBinding

    private val authViewModel : AuthViewModel by viewModels()

    @Inject
    lateinit var tokenManager: TokenManager

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = FragmentLoginBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        listeners()
        observers()
    }

    private fun listeners(){

        with(binding){
            btnLogin.setOnClickListener{
                binding.progressBar.visible()
                val email = etEmail.text.toString()
                val password = etPassword.text.toString()

                authViewModel.login(email, password)
            }

            tvForgotPassword.setOnClickListener{
                findNavController().navigate(R.id.action_loginFragment_to_forgotPasswordFragment)
            }

        }
    }

    private fun observers(){

        authViewModel.loginLiveData.observe(viewLifecycleOwner, Observer{response->

            when(response){

                is NetworkResult.Success ->{
                    response.data?.let { user->
                        binding.progressBar.gone()

                        val intent = Intent(requireActivity(), HomeActivity::class.java)
                        startActivity(intent)
                        requireActivity().finish()
                    }
                }

                is NetworkResult.Error ->{
                    Toast.makeText(context, "Error: ${response.message}", Toast.LENGTH_SHORT).show()
                    binding.progressBar.gone()
                }

                is NetworkResult.Loading ->{}
            }
        })

    }
}