package com.adsperclick.media.views.user.fragment

import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.adsperclick.media.R
import com.adsperclick.media.applicationCommonView.view.EditeTextWithError
import com.adsperclick.media.data.dataModels.CommonData
import com.adsperclick.media.data.dataModels.Company
import com.adsperclick.media.data.dataModels.NetworkResult
import com.adsperclick.media.data.dataModels.Service
import com.adsperclick.media.data.dataModels.User
import com.adsperclick.media.databinding.FragmentFormBinding
import com.adsperclick.media.utils.Constants
import com.adsperclick.media.utils.disableSubmitButton
import com.adsperclick.media.utils.enableSubmitButton
import com.adsperclick.media.utils.visible
import com.adsperclick.media.views.user.bottomsheet.ServiceBottomSheetFragment
import com.adsperclick.media.views.user.viewmodel.UserViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.util.Date
import java.util.Locale

@AndroidEntryPoint
class FormFragment : Fragment(),View.OnClickListener {

    private lateinit var binding: FragmentFormBinding
    private lateinit var userType:String
    private val viewModel : UserViewModel by viewModels()

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
        collectUiData()
        setUpTitle()
        setUpHint()
        setUpPlaceholder()
        setUpVisibility()
        setUpInputType()
        setUpClickListener()
        setUpDrawable()
        setUpErrorPadding()
        setUpObserver()
        setCustomTextWatchers()
        validateSubmitButton()
    }
    private fun collectUiData(){
        when(userType){
            Constants.COMPANIES_SEMI_CAPS -> {
                viewModel.getServiceList()
            }
            Constants.CLIENTS_SEMI_CAPS -> {
                viewModel.getCompanyList()
            }
        }

    }

    private fun setUpTitle(){
        binding.title.text = when(userType){
            Constants.COMPANIES_SEMI_CAPS -> getString(R.string.add_company)
            Constants.CLIENTS_SEMI_CAPS -> getString(R.string.add_client)
            Constants.EMPLOYEES_SEMI_CAPS -> getString(R.string.add_employee)
            Constants.SERVICES_SEMI_CAPS -> getString(R.string.add_service)
            else -> { getString(R.string.error) }
        }
    }

    private fun setUpHint(){
        with(binding){
            firstName.setHint(R.string.first_name)
            lastName.setHint(R.string.last_name)
            companyName.setHint(R.string.company_name)
            selectCompany.setHint(R.string.select_company)
            gst.setHint(R.string.gst_number)
            email.setHint(R.string.email)
            aadharNumber.setHint(R.string.aadhar_card)
            password.setHint(R.string.password)
            confirmPassword.setHint(R.string.confirm_password)
            servicesSubscribed.setHint(R.string.services_subscribed)
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
                    gst.getEditView().apply {
                        isClickable = true
                        isFocusable = false
                    }
                }
                Constants.SERVICES_SEMI_CAPS ->{
                    serviceName.visible()
                }
                Constants.COMPANIES_SEMI_CAPS ->{
                    companyName.visible()
                    gst.visible()
                    servicesSubscribed.visible()
                    servicesSubscribed.getEditView().apply {
                        isClickable = true
                        isFocusable = false
                        setOnClickListener {
                            gst.getEditView().clearFocus()
                            companyName.getEditView().clearFocus()
                            it.post { openSelectServiceBottomSheet() }
                        }
                    }
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
            servicesSubscribed.setInputType(InputType.TYPE_CLASS_TEXT)
            serviceName.setInputType(InputType.TYPE_CLASS_TEXT)
        }
    }

    private fun setUpClickListener(){
        binding.submitButton.setOnClickListener(this)
        binding.btnBack.setOnClickListener(this)
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
            servicesSubscribed.setErrorText(Constants.SPACE)
            serviceName.setErrorText(Constants.SPACE)

            firstName.removeErrorText()
            lastName.removeErrorText()
            companyName.removeErrorText()
            gst.removeErrorText()
            email.removeErrorText()
            aadharNumber.removeErrorText()
            password.removeErrorText()
            confirmPassword.removeErrorText()
            servicesSubscribed.removeErrorText()
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
                            selectCompany.getSelectedItem()?.isNotEmpty() == true &&
                            gst.getText()?.isNotEmpty() == true &&
                            email.getText()?.isNotEmpty() == true &&
                            password.getText()?.isNotEmpty() == true &&
                            confirmPassword.getText()?.isNotEmpty() == true
                }

                Constants.SERVICES_SEMI_CAPS -> {
                    serviceName.getText()?.isNotEmpty() == true
                }

                Constants.COMPANIES_SEMI_CAPS -> {
                    companyName.getText()?.isNotEmpty() == true &&
                            gst.getText()?.isNotEmpty() == true &&
                            servicesSubscribed.getText()?.isNotEmpty() == true
                }

                else -> false
            }
        }
    }

    fun validateSubmitButton() {
        if (areFixedDetailsValid(userType)) {
            binding.submitButton.enableSubmitButton()
        } else {
            binding.submitButton.disableSubmitButton()
        }
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
            servicesSubscribed.getEditView().addTextChangedListener(
                FormTextWatcher(
                    this@FormFragment,
                    viewModel = viewModel,
                    view = servicesSubscribed,
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
            binding.selectCompany.getSpinnerView().doAfterTextChanged {
                val enteredText = it.toString().trim()
                viewModel.selectedCompany = viewModel.companyList.find { company ->
                    company.companyName.equals(enteredText, ignoreCase = true)
                }
                viewModel.selectedCompany?.gstNumber?.let { it1 -> binding.gst.setText(it1) }
                validateSubmitButton()
            }
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
            var selfCompanyName: String? = null
            var selfCompanyId:String? = null
            val simpleDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val currentDate: String = simpleDateFormat.format(Date())



            // Assign values based on userType and visibility of Groups
            when (userType) {
                Constants.EMPLOYEES_SEMI_CAPS -> {
                    userRole = Constants.ROLE.EMPLOYEE
                    aadharNumber = viewModel.aadharNumber
                }

                Constants.CLIENTS_SEMI_CAPS -> {
                    userRole=Constants.ROLE.CLIENT
                    selfCompanyId = viewModel.selectedCompany?.companyId
                    selfCompanyName = viewModel.selectedCompany?.companyName
                    gstNumber = viewModel.gstNumber
                }

                Constants.SERVICES_SEMI_CAPS -> {
                    serviceName = viewModel.serviceName
                }

                Constants.COMPANIES_SEMI_CAPS -> {
                    companyName = viewModel.companyName
                    gstNumber = viewModel.gstNumber
                }
            }

            // Construct the User object based on available details



            when (userType) {
                Constants.EMPLOYEES_SEMI_CAPS, Constants.CLIENTS_SEMI_CAPS -> {
                    val user = User(
                        userName = firstName?.plus(" ")?.plus(lastName ?: ""),
                        email = email,
                        password = password,
                        role = userRole,
                        userAdhaarNumber = aadharNumber,
                        selfCompanyId = selfCompanyId,
                        selfCompanyName = selfCompanyName,
                        selfCompanyGstNumber = gstNumber,
                        associationDate = currentDate
                    )
                    viewModel.registerUser(user)
                }

                Constants.SERVICES_SEMI_CAPS -> {
                    val service = Service(
                        serviceName = serviceName
                    )
                    viewModel.registerService(service)

                }

                Constants.COMPANIES_SEMI_CAPS -> {
                    val company = Company(
                        companyName = companyName,
                        gstNumber = gstNumber,
                        listOfServices = convertCommonDataToService(viewModel.selectServiceList)
                    )
                    viewModel.registerCompany(company)
                }
            }
        }
    }

    private fun setUpObserver(){

        viewModel.listServiceLiveData.observe(viewLifecycleOwner,serviceListObserver)
        viewModel.listCompanyLiveData.observe(viewLifecycleOwner,companyListObserver)

        viewModel.registrationLiveData.observe(viewLifecycleOwner, Observer{response->

            when(response){

                is NetworkResult.Success ->{
                    successMessage()
                }

                is NetworkResult.Error ->{
                    Toast.makeText(context, "Error: ${response.message}", Toast.LENGTH_SHORT).show()
                }

                is NetworkResult.Loading ->{}
            }
        })

        viewModel.registerCompanyLiveData.observe(viewLifecycleOwner, Observer{response->

            when(response){

                is NetworkResult.Success ->{
                    successMessage()
                }

                is NetworkResult.Error ->{
                    Toast.makeText(context, "Error: ${response.message}", Toast.LENGTH_SHORT).show()
                }

                is NetworkResult.Loading ->{}
            }
        })

        viewModel.registerServiceLiveData.observe(viewLifecycleOwner, Observer{response->

            when(response){

                is NetworkResult.Success ->{
                    successMessage()
                }

                is NetworkResult.Error ->{
                    response.message?.let { failedMessage() }
                    Toast.makeText(context, "Error: ${response.message}", Toast.LENGTH_SHORT).show()
                }

                is NetworkResult.Loading ->{}
            }
        })
    }

    private val serviceListObserver = Observer<NetworkResult<ArrayList<Service>>> {
        when(it){

            is NetworkResult.Success ->{
                it.data?.let { it1 -> viewModel.serviceList = convertServiceToCommonData(it1) }
            }

            is NetworkResult.Error ->{
                Toast.makeText(context, "Error: ${it.message}", Toast.LENGTH_SHORT).show()
            }

            is NetworkResult.Loading ->{}
        }
    }

    private val companyListObserver = Observer<NetworkResult<ArrayList<Company>>> {
        when(it){

            is NetworkResult.Success ->{
                it.data?.let { it1 -> viewModel.companyList = it1 }
                binding.selectCompany.setDataItemList(viewModel.companyList.mapNotNull { it.companyName })
            }

            is NetworkResult.Error ->{
                Toast.makeText(context, "Error: ${it.message}", Toast.LENGTH_SHORT).show()
            }

            is NetworkResult.Loading ->{}
        }

    }

    private fun convertServiceToCommonData(serviceList: List<Service>): List<CommonData> {
        return serviceList.map { service ->
            CommonData(
                id = service.serviceId ?: "",
                name = service.serviceName ?: ""
            )
        }
    }


    private fun convertCommonDataToService(serviceList: List<CommonData>): List<Service> {
        return serviceList.map { service ->
            Service(
                serviceId = service.id ?: "",
                serviceName = service.name ?: ""
            )
        }
    }

    private fun successMessage(){
        binding.submitButton.apply {
            text = getString(R.string.success)
            backgroundTintList = ContextCompat.getColorStateList(requireContext(), R.color.green_info)
            isEnabled = false
        }

        lifecycleScope.launch {
            delay(1000)
            findNavController().popBackStack()
        }
    }

    private fun failedMessage() {
        binding.submitButton.apply {
            text = getString(R.string.failed)
            backgroundTintList = ContextCompat.getColorStateList(requireContext(), R.color.red_email)
            isEnabled = false
        }

        lifecycleScope.launch {
            delay(1000) // Show the error message for 1 second
            binding.submitButton.apply {
                text = getString(R.string.submit) // Change back to original text
                backgroundTintList = ContextCompat.getColorStateList(requireContext(), R.color.blue_common_button)
                isEnabled = true
            }
        }
    }

    private val listener = object : ServiceBottomSheetFragment.MultiSelectListener{
        override fun onMultiSelect(
            bucketName: String,
            selectedList: ArrayList<CommonData>
        ) {
            var valueData = Constants.EMPTY
            selectedList.forEachIndexed { index, value ->
                if(value.name?.isNotEmpty() == true){
                    valueData += if (index!=selectedList.size -1){
                        "${value.name}, "
                    }else{
                        value.name
                    }
                }
            }
            viewModel.selectServiceList = selectedList
            binding.servicesSubscribed.setText(valueData)
            binding.servicesSubscribed.getEditView().setSelection(valueData.length)
        }
    }

    private fun openSelectServiceBottomSheet(){
        val bottomSheet = ServiceBottomSheetFragment.newInstance(userType,listener,viewModel.serviceList,viewModel.selectServiceList)
        bottomSheet.isCancelable = false
        bottomSheet.show(childFragmentManager, bottomSheet.tag)
    }

    private fun validatePasswords(): Boolean {
        val passwordText = binding.password.getText() ?:""
        val confirmPasswordText = binding.confirmPassword.getText() ?: ""

        // Check password length first
        if (passwordText.length < 6) {
            binding.password.setErrorText(getString(R.string.password_length))
            return false
        }

        if (confirmPasswordText.length < 6) {
            binding.confirmPassword.setErrorText(getString(R.string.password_length))
            return false
        }

        // Then check if passwords match
        return if (passwordText == confirmPasswordText) {
            binding.confirmPassword.removeErrorText()
            true
        } else {
            binding.confirmPassword.setErrorText(getString(R.string.password_mismatch))
            false
        }
    }

    override fun onClick(v: View?) {
        when(v){
            binding.submitButton -> {
                if(areFixedDetailsValid(userType)){
                    if (validatePasswords() && ( userType == Constants.CLIENTS_SEMI_CAPS || userType == Constants.EMPLOYEES_SEMI_CAPS)) {
                        saveUserDetails(userType)
                    }
                }
            }
            binding.btnBack -> {
                findNavController().popBackStack()
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
            R.id.first_name -> handleValidation(inputText, regexPattern, errorMessage) { viewModel.firstName = it }
            R.id.last_name -> handleValidation(inputText, regexPattern, errorMessage) { viewModel.lastName = it }
            R.id.company_name -> handleValidation(inputText, regexPattern, errorMessage) { viewModel.companyName = it }
            R.id.gst -> handleValidation(inputText, regexPattern, errorMessage) { viewModel.gstNumber = it }
            R.id.aadhar_number -> handleValidation(inputText, regexPattern, errorMessage) { viewModel.aadharNumber = it }
            R.id.email -> handleValidation(inputText, regexPattern, errorMessage) { viewModel.email = it }
            R.id.password -> handleValidation(inputText, regexPattern, errorMessage) { viewModel.password = it }
            R.id.confirm_password -> handleValidation(inputText, regexPattern, errorMessage) { viewModel.confirmPassword = it }
            R.id.services_subscribed -> handleValidation(inputText, regexPattern, errorMessage) { viewModel.services = it }
            R.id.service_name -> handleValidation(inputText, regexPattern, errorMessage) { viewModel.serviceName = it }
        }
        fragment.validateSubmitButton()
    }

    private fun handleValidation(
        inputText: String,
        regexPattern: String,
        errorMessage: String,
        updateViewModel: (String) -> Unit
    ) {
        val context = view.context
        when {
            inputText.isEmpty() -> {
                view.setErrorText(errorMessage)
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
