package com.adsperclick.media.views.user.fragment

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.adsperclick.media.R
import com.adsperclick.media.data.dataModels.CommonData
import com.adsperclick.media.data.dataModels.Company
import com.adsperclick.media.data.dataModels.NetworkResult
import com.adsperclick.media.data.dataModels.Service
import com.adsperclick.media.data.dataModels.User
import com.adsperclick.media.databinding.FragmentSettingBinding
import com.adsperclick.media.databinding.FragmentUserInfoBinding
import com.adsperclick.media.utils.Constants
import com.adsperclick.media.utils.UtilityFunctions
import com.adsperclick.media.utils.disableHeaderButton
import com.adsperclick.media.utils.enableHeaderButton
import com.adsperclick.media.utils.gone
import com.adsperclick.media.views.user.bottomsheet.ServiceBottomSheetFragment
import com.adsperclick.media.views.user.viewmodel.UserViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class UserInfoFragment : Fragment(), View.OnClickListener {
    private lateinit var binding: FragmentUserInfoBinding
    private lateinit var userType:String
    private var userId:String?=null
    private var user: User?=null
    private var company: Company?=null

    private val viewModel: UserViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            userId = it.getString(Constants.USER_ID).toString()
            userType = it.getString(Constants.USER_TYPE_SEMI_CAPS).toString()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        // Inflate the layout for this fragment
        binding = FragmentUserInfoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setUpHeader()
        uiDataCollect()
        setUpObserver()
        setUpVisibility()
        setUpClickListener()
    }

    private fun setUpHeader(){
        binding.header.tvTitle.text = userType
    }

    private fun uiDataCollect() {

        when(userType) {
            Constants.EMPLOYEES_SEMI_CAPS, Constants.CLIENTS_SEMI_CAPS -> {
                viewModel.getUserData(userId!!).observe(viewLifecycleOwner) { result ->
                    when(result) {
                        is NetworkResult.Success -> {
                            // Store user data and update UI
                            user = result.data
                            // Use userData as needed
                            setUpData()
                        }
                        is NetworkResult.Error -> {
                            // Handle error
                            Toast.makeText(requireContext(), result.message, Toast.LENGTH_SHORT).show()
                        }
                        is NetworkResult.Loading -> {
                            // Show loading state
                        }
                    }
                }
            }
            Constants.COMPANIES_SEMI_CAPS -> {
                viewModel.getServiceList()
                viewModel.getCompanyData(userId!!).observe(viewLifecycleOwner) { result ->
                    when(result) {
                        is NetworkResult.Success -> {
                            // Store company data and update UI
                            company = result.data
                            // Use companyData as needed
                            setUpData()
                        }
                        is NetworkResult.Error -> {
                            // Handle error
                            Toast.makeText(requireContext(), result.message, Toast.LENGTH_SHORT).show()
                        }
                        is NetworkResult.Loading -> {
                            // Show loading state
                        }
                    }
                }
            }
        }
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

    private fun setUpData(){
        when(userType){
            Constants.EMPLOYEES_SEMI_CAPS,Constants.CLIENTS_SEMI_CAPS->{
                binding.tvName.text = user?.userName ?: "N.A."
                binding.tvEmail.text = user?.email ?: "N.A."
                binding.tvPhone.text = user?.userPhoneNumber ?: "N.A."
                binding.tvAssociationDate.text = user?.associationDate ?: "N.A."
                user?.userProfileImgUrl?.let { imageUrl ->
                    UtilityFunctions.loadImageWithGlide(
                        binding.imgProfileDp.context,
                        binding.imgProfileDp,
                        imageUrl
                    )
                } ?: run {
                    UtilityFunctions.setInitialsDrawable(
                        binding.imgProfileDp,
                        user?.userName
                    )
                }
            }
            Constants.COMPANIES_SEMI_CAPS->{
                binding.tvName.text = company?.companyName ?: "N.A."
                UtilityFunctions.setInitialsDrawable(
                    binding.imgProfileDp,
                    company?.companyName ?: "N.A."
                )
                company?.listOfServices?.forEach { service ->
                    viewModel.selectServiceList.add(CommonData(
                        id = service.serviceId,
                        name = service.serviceName
                        // Add other required properties
                    ))
                }
                binding.tvServices.text = binding.tvServices.context.getString(R.string.size,viewModel.selectServiceList.size)
                validateSubmitButton()

            }
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

    private fun setUpVisibility(){
        when(userType){
            Constants.EMPLOYEES_SEMI_CAPS,Constants.CLIENTS_SEMI_CAPS->{
                binding.cvServices.gone()
                binding.header.btnSave.gone()
            }
            Constants.COMPANIES_SEMI_CAPS->{
                binding.cvAssociationDate.gone()
                binding.cvEmail.gone()
                binding.btnBlock.gone()
                binding.cvPhone.gone()
            }
        }
    }

    private fun setUpClickListener(){
        binding.header.btnBack.setOnClickListener(this)
        binding.cvServices.setOnClickListener(this)
        binding.header.btnSave.setOnClickListener(this)
    }

    private val listener = object : ServiceBottomSheetFragment.MultiSelectListener{
        override fun onMultiSelect(
            bucketName: String,
            selectedList: ArrayList<CommonData>
        ) {

            viewModel.selectServiceList = selectedList
            binding.tvServices.text = binding.tvServices.context.getString(R.string.size,selectedList.size)
            validateSubmitButton()
        }
    }

    private fun openSelectServiceBottomSheet(){
        val bottomSheet = ServiceBottomSheetFragment.newInstance(userType,listener,viewModel.serviceList,viewModel.selectServiceList)
        bottomSheet.isCancelable = false
        bottomSheet.show(childFragmentManager, bottomSheet.tag)
    }

    private fun areFixedDetailsChanged():Boolean{
        return company?.listOfServices?.size!=viewModel.selectServiceList.size
    }

    private fun validateSubmitButton() {
        if (areFixedDetailsChanged()) {
            binding.header.btnSave.enableHeaderButton()
        } else {
            binding.header.btnSave.disableHeaderButton()
        }
    }

    private fun setUpObserver(){
        viewModel.listServiceLiveData.observe(viewLifecycleOwner,serviceListObserver)
        viewModel.updateCompanyServicesLiveData.observe(viewLifecycleOwner,updateCompanyServiceListObserver)
    }

    private val updateCompanyServiceListObserver = Observer<NetworkResult<Boolean>> {
        when(it){
            is NetworkResult.Success ->{
                Toast.makeText(requireContext(), "Services updated successfully", Toast.LENGTH_SHORT).show()
                // Update local company object to reflect changes
                company = company?.copy(
                    listOfServices = convertCommonDataToService(viewModel.selectServiceList)
                )
                validateSubmitButton()
            }

            is NetworkResult.Error ->{
                binding.header.btnSave.enableHeaderButton() // Re-enable button on error
                Toast.makeText(requireContext(), it.message, Toast.LENGTH_SHORT).show()
            }

            is NetworkResult.Loading ->{}
        }
    }

    private fun updateProfile() {
        if (userType == Constants.COMPANIES_SEMI_CAPS && company != null) {
            company?.let {
                viewModel.updateCompanyServices(it.companyId!!, convertCommonDataToService(viewModel.selectServiceList))
            }
        }
    }

    override fun onClick(v: View?) {
        when(v){
            binding.header.btnBack -> {
                findNavController().popBackStack()
            }
            binding.cvServices -> {
                openSelectServiceBottomSheet()
            }
            binding.header.btnSave->{
                if(areFixedDetailsChanged()){
                    binding.header.btnSave.disableHeaderButton()
                    updateProfile()
                }

            }
        }
    }

}