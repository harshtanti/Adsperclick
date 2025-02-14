package com.adsperclick.media.views.user.fragment

import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.adsperclick.media.R
import com.adsperclick.media.applicationCommonView.view.EditeTextWithError
import com.adsperclick.media.data.dataModels.User
import com.adsperclick.media.databinding.FragmentFormBinding
import com.adsperclick.media.utils.Constants
import com.adsperclick.media.utils.gone
import com.adsperclick.media.utils.visible
import com.adsperclick.media.views.login.viewModels.AuthViewModel
import com.adsperclick.media.views.user.viewmodel.UserViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class FormFragment : Fragment(),View.OnClickListener {

    private lateinit var binding: FragmentFormBinding
    private lateinit var userType:String
    private val viewModel : UserViewModel by viewModels()

    private val authViewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            userType = it.getString(Constants.USER_TYPE_SEMI_CAPS).toString()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentFormBinding.inflate(inflater,container,false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setUpTitle()
        setUpHint()
        setUpPlaceholder()
        setUpVisibility()
        setUpInputType()
        setUpClickListener()
        setUpDrawable()
        setUpErrorPadding()
        setCustomTextWatchers()
        validateSubmitButton()
    }

    private fun setUpTitle(){
        binding.title.text=userType
    }

    private fun setUpHint(){
        with(binding){
            firstName.setHint(R.string.first_name)
            lastName.setHint(R.string.last_name)
            companyName.setHint(R.string.company_name)
            gst.setHint(R.string.gst_number)
            email.setHint(R.string.email)
            aadharNumber.setHint(R.string.aadhar_card)
            password.setHint(R.string.password)
            confirmPassword.setHint(R.string.confirm_password)
            services.setHint(R.string.services_subscribed)
            serviceName.setHint(R.string.services_name)
            email.setPlaceHolderText(getString(R.string.example_gmail))
        }
    }

    private fun setUpPlaceholder(){
        with(binding){
            email.setPlaceHolderText(getString(R.string.example_gmail))
        }
    }

    private fun setUpVisibility(){
        with(binding){
            when(userType) {
                Constants.EMPLOYEES_SEMI_CAPS ->{
                    employeeGroup.visible()
                }
                Constants.CLIENTS_SEMI_CAPS ->{
                    clientGroup.visible()
                }
                Constants.SERVICES_SEMI_CAPS ->{
                    serviceName.visible()
                }
                Constants.COMPANIES_SEMI_CAPS ->{
                    companyName.visible()
                }
                else -> {

                }
            }
        }
    }

    private fun setUpInputType() {
        with(binding) {
            firstName.setInputType(InputType.TYPE_CLASS_TEXT)
            lastName.setInputType(InputType.TYPE_CLASS_TEXT)
            companyName.setInputType(InputType.TYPE_CLASS_TEXT)
            gst.setInputType(InputType.TYPE_CLASS_TEXT)
            email.setInputType(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS)
            aadharNumber.setInputType(InputType.TYPE_CLASS_NUMBER)
            password.setInputType(InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD)
            confirmPassword.setInputType(InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD)
            services.setInputType(InputType.TYPE_CLASS_TEXT)
            serviceName.setInputType(InputType.TYPE_CLASS_TEXT)
        }
    }

    private fun setUpClickListener(){
        binding.submitButton.setOnClickListener(this)
    }

    private fun setUpErrorPadding(){
        with(binding) {
            firstName.setErrorText(Constants.SPACE)
            lastName.setErrorText(Constants.SPACE)
            companyName.setErrorText(Constants.SPACE)
            gst.setErrorText(Constants.SPACE)
            email.setErrorText(Constants.SPACE)
            aadharNumber.setErrorText(Constants.SPACE)
            password.setErrorText(Constants.SPACE)
            confirmPassword.setErrorText(Constants.SPACE)
            services.setErrorText(Constants.SPACE)
            serviceName.setErrorText(Constants.SPACE)

            firstName.removeErrorText()
            lastName.removeErrorText()
            companyName.removeErrorText()
            gst.removeErrorText()
            email.removeErrorText()
            aadharNumber.removeErrorText()
            password.removeErrorText()
            confirmPassword.removeErrorText()
            services.removeErrorText()
            serviceName.removeErrorText()
        }
    }

    private fun setUpDrawable(){
        with(binding) {
            password.setStartIcon(password.context,R.drawable.ic_lock)
            password.enablePasswordToggle()

            confirmPassword.setStartIcon(confirmPassword.context,R.drawable.ic_lock)
            confirmPassword.enablePasswordToggle()

            email.setStartIcon(email.context,R.drawable.ic_email)
        }
    }

    private fun areFixedDetailsValid(userType: String): Boolean {
        with(binding) {
            return when (userType) {
                Constants.EMPLOYEES_SEMI_CAPS -> {
                    firstName.getText()?.isNotEmpty() == true &&
                            lastName.getText()?.isNotEmpty() == true &&
                            aadharNumber.getText()?.isNotEmpty() == true &&
                            email.getText()?.isNotEmpty() == true &&
                            password.getText()?.isNotEmpty() == true &&
                            confirmPassword.getText()?.isNotEmpty() == true
                }

                Constants.CLIENTS_SEMI_CAPS -> {
                    firstName.getText()?.isNotEmpty() == true &&
                            lastName.getText()?.isNotEmpty() == true &&
                            companyName.getText()?.isNotEmpty() == true &&
                            gst.getText()?.isNotEmpty() == true &&
                            email.getText()?.isNotEmpty() == true &&
                            password.getText()?.isNotEmpty() == true &&
                            confirmPassword.getText()?.isNotEmpty() == true
                }

                Constants.SERVICES_SEMI_CAPS -> {
                    serviceName.getText()?.isNotEmpty() == true
                }

                Constants.COMPANIES_SEMI_CAPS -> {
                    companyName.getText()?.isNotEmpty() == true
                }

                else -> false
            }
        }
    }

    fun validateSubmitButton() {
        if (areFixedDetailsValid(userType)) {
            enableSubmitButton()
        } else {
            disableSubmitButton()
        }
    }

    private fun disableSubmitButton() {
        binding.submitButton.backgroundTintList = ContextCompat.getColorStateList(
            requireContext(), R.color.disabled_color
        )
        binding.submitButton.isEnabled = false
    }

    private fun enableSubmitButton() {
        binding.submitButton.backgroundTintList = ContextCompat.getColorStateList(
            requireContext(), R.color.blue_common_button
        )
        binding.submitButton.isEnabled = true
    }

    private fun regexMatch(text:String,regexPattern:String): Boolean {
        val pattern = Regex(regexPattern)
        return pattern.matches(text)
    }

    private fun setCustomTextWatchers(){
        with(binding){
            firstName.getEditView().addTextChangedListener(
                FormTextWatcher(
                    this@FormFragment,
                    viewModel = viewModel,
                    view = firstName,
                    errorMessage = getString(R.string.first_name_required),
                    regexPattern = Constants.EMPTY
                )
            )
            lastName.getEditView().addTextChangedListener(
                FormTextWatcher(
                    this@FormFragment,
                    viewModel = viewModel,
                    view = lastName,
                    errorMessage = getString(R.string.last_name_required),
                    regexPattern = Constants.EMPTY
                )
            )
            companyName.getEditView().addTextChangedListener(
                FormTextWatcher(
                    this@FormFragment,
                    viewModel = viewModel,
                    view = companyName,
                    errorMessage = getString(R.string.company_name_required),
                    regexPattern = Constants.EMPTY
                )
            )
            gst.getEditView().addTextChangedListener(
                FormTextWatcher(
                    this@FormFragment,
                    viewModel = viewModel,
                    view = gst,
                    errorMessage = getString(R.string.gst_number_is_incorrect),
                    regexPattern = Constants.EMPTY
                )
            )
            aadharNumber.getEditView().addTextChangedListener(
                FormTextWatcher(
                    this@FormFragment,
                    viewModel = viewModel,
                    view = aadharNumber,
                    errorMessage = getString(R.string.aadhar_number_is_incorrect),
                    regexPattern = Constants.EMPTY
                )
            )
            email.getEditView().addTextChangedListener(
                FormTextWatcher(
                    this@FormFragment,
                    viewModel = viewModel,
                    view = email,
                    errorMessage = getString(R.string.email_is_incorrect),
                    regexPattern = Constants.EMPTY
                )
            )
            password.getEditView().addTextChangedListener(
                FormTextWatcher(
                    this@FormFragment,
                    viewModel = viewModel,
                    view = password,
                    errorMessage = getString(R.string.password_required),
                    regexPattern = Constants.EMPTY
                )
            )
            confirmPassword.getEditView().addTextChangedListener(
                FormTextWatcher(
                    this@FormFragment,
                    viewModel = viewModel,
                    view = confirmPassword,
                    errorMessage = getString(R.string.confirm_password_required),
                    regexPattern = Constants.EMPTY
                )
            )
            services.getEditView().addTextChangedListener(
                FormTextWatcher(
                    this@FormFragment,
                    viewModel = viewModel,
                    view = services,
                    errorMessage = getString(R.string.services_required),
                    regexPattern = Constants.EMPTY
                )
            )
            serviceName.getEditView().addTextChangedListener(
                FormTextWatcher(
                    this@FormFragment,
                    viewModel = viewModel,
                    view = serviceName,
                    errorMessage = getString(R.string.service_name_required),
                    regexPattern = Constants.EMPTY
                )
            )
        }
    }

    private fun saveUserDetails(userType: String) {
        with(binding) {
            val firstName = viewModel.firstName
            val lastName = viewModel.lastName
            val email = viewModel.email
            val password = viewModel.password
            val confirmPassword = viewModel.confirmPassword

            // Initialize variables with null by default
            var aadharNumber: String? = null
            var companyName: String? = null
            var gstNumber: String? = null
            var serviceName: String? = null
            var userRole: Int? = null

            // Assign values based on userType and visibility of Groups
            when (userType) {
                Constants.EMPLOYEES_SEMI_CAPS -> {
                    userRole=Constants.EMPLOYEE
                    aadharNumber = viewModel.aadharNumber
                }

                Constants.CLIENTS_SEMI_CAPS -> {
                    userRole=Constants.CLIENT
                    companyName = viewModel.companyName
                    gstNumber = viewModel.gstNumber
                }

                Constants.SERVICES_SEMI_CAPS -> {
                    serviceName = viewModel.serviceName
                }

                Constants.COMPANIES_SEMI_CAPS -> {
                    companyName = viewModel.companyName
                }
            }

            // Construct the User object based on available details
            val user = User(
                userName = firstName?.plus(" ")?.plus(lastName ?: ""),
                email = email,
                password = password,
                role = userRole,
                userAdhaarNumber = aadharNumber,
                selfCompanyName = companyName,
                selfCompanyGstNumber = gstNumber
            )

            when (userType) {
                Constants.EMPLOYEES_SEMI_CAPS, Constants.CLIENTS_SEMI_CAPS -> {
                    authViewModel.register(user)
                }

                Constants.SERVICES_SEMI_CAPS -> {

                }

                Constants.COMPANIES_SEMI_CAPS -> {

                }
            }
        }
    }

    override fun onClick(v: View?) {
        when(v){
            binding.submitButton -> {
                if(areFixedDetailsValid(userType)){
                    saveUserDetails(userType)
                    findNavController().popBackStack()
                }
            }
        }
    }

}

class FormTextWatcher(
    private val fragment: FormFragment,
    private val viewModel: UserViewModel, // ViewModel to update fields
    private val view: EditeTextWithError, // Custom EditText view
    private val errorMessage: String, // Error message to display
    private val regexPattern: String // Validation regex pattern
) : TextWatcher {



    override fun afterTextChanged(s: Editable?) {
        val inputText = s?.toString()?.trim() ?: Constants.EMPTY
        val inputLayout = view.getEditView()

        when (view.id) {
            R.id.first_name -> handleValidation(inputText, regexPattern, R.string.first_name_required) { viewModel.firstName = it }
            R.id.last_name -> handleValidation(inputText, regexPattern, R.string.last_name_required) { viewModel.lastName = it }
            R.id.company_name -> handleValidation(inputText, regexPattern, R.string.company_name_required) { viewModel.companyName = it }
            R.id.gst -> handleValidation(inputText, regexPattern, R.string.gst_number_is_incorrect) { viewModel.gstNumber = it }
            R.id.aadhar_number -> handleValidation(inputText, regexPattern, R.string.aadhar_number_is_incorrect) { viewModel.aadharNumber = it }
            R.id.email -> handleValidation(inputText, regexPattern, R.string.email_is_incorrect) { viewModel.email = it }
            R.id.password -> handleValidation(inputText, regexPattern, R.string.password_required) { viewModel.password = it }
            R.id.confirm_password -> handleValidation(inputText, regexPattern, R.string.confirm_password_required) { viewModel.confirmPassword = it }
            R.id.services -> handleValidation(inputText, regexPattern, R.string.services_required) { viewModel.services = it }
            R.id.service_name -> handleValidation(inputText, regexPattern, R.string.service_name_required) { viewModel.serviceName = it }
        }
        fragment.validateSubmitButton()
    }

    private fun handleValidation(
        inputText: String,
        regexPattern: String,
        errorResId: Int,
        updateViewModel: (String) -> Unit
    ) {
        val context = view.context
        when {
            inputText.isEmpty() -> {
                view.setErrorText(context.getString(errorResId))
            }
            else -> {
                view.removeErrorText()
                updateViewModel(inputText)
            }
        }
    }

    private fun isValidInput(text: String, regexPattern: String): Boolean {
        return Regex(regexPattern).matches(text)
    }

    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

}
