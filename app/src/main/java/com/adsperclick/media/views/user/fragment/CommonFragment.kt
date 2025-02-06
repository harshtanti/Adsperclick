package com.adsperclick.media.views.user.fragment

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.adsperclick.media.R
import com.adsperclick.media.data.dataModels.CommonData
import com.adsperclick.media.databinding.FragmentCommonBinding
import com.adsperclick.media.utils.Constants
import com.adsperclick.media.views.user.adapter.CommonAdapter

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [CommonFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class CommonFragment : Fragment(), View.OnClickListener {
    // TODO: Rename and change types of parameters
    private var tabName: String? = null
    private lateinit var binding: FragmentCommonBinding
    private lateinit var adapter: CommonAdapter
    private var listener: CommonAdapter.CommunityListener ?= null

    private val allData = listOf(
        // Employees (20)
        CommonData(id = "1", name = "John Doe", tagName = "Employee"),
        CommonData(id = "2", name = "Jane Smith", tagName = "Employee"),
        CommonData(id = "3", name = "Robert Brown", tagName = "Employee"),
        CommonData(id = "4", name = "Emily Davis", tagName = "Employee"),
        CommonData(id = "5", name = "Michael Johnson", tagName = "Employee"),
        CommonData(id = "6", name = "Sarah Lee", tagName = "Employee"),
        CommonData(id = "7", name = "David Wilson", tagName = "Employee"),
        CommonData(id = "8", name = "Olivia Martinez", tagName = "Employee"),
        CommonData(id = "9", name = "James Anderson", tagName = "Employee"),
        CommonData(id = "10", name = "Sophia Thomas", tagName = "Employee"),
        CommonData(id = "11", name = "Daniel White", tagName = "Employee"),
        CommonData(id = "12", name = "Emma Black", tagName = "Employee"),
        CommonData(id = "13", name = "Liam Harris", tagName = "Employee"),
        CommonData(id = "14", name = "Noah King", tagName = "Employee"),
        CommonData(id = "15", name = "Isabella Scott", tagName = "Employee"),
        CommonData(id = "16", name = "Lucas Carter", tagName = "Employee"),
        CommonData(id = "17", name = "Mia Adams", tagName = "Employee"),
        CommonData(id = "18", name = "Ethan Clark", tagName = "Employee"),
        CommonData(id = "19", name = "Charlotte Baker", tagName = "Employee"),
        CommonData(id = "20", name = "Mason Turner", tagName = "Employee"),

        // Clients (20)
        CommonData(id = "21", name = "Alice White", tagName = "Client"),
        CommonData(id = "22", name = "Bob Green", tagName = "Client"),
        CommonData(id = "23", name = "Charlie Black", tagName = "Client"),
        CommonData(id = "24", name = "Daniel Harris", tagName = "Client"),
        CommonData(id = "25", name = "Eva Scott", tagName = "Client"),
        CommonData(id = "26", name = "Frank Adams", tagName = "Client"),
        CommonData(id = "27", name = "Grace Baker", tagName = "Client"),
        CommonData(id = "28", name = "Henry Carter", tagName = "Client"),
        CommonData(id = "29", name = "Isabella King", tagName = "Client"),
        CommonData(id = "30", name = "Jack Turner", tagName = "Client"),
        CommonData(id = "31", name = "Sophia Williams", tagName = "Client"),
        CommonData(id = "32", name = "Benjamin Miller", tagName = "Client"),
        CommonData(id = "33", name = "Lucas Evans", tagName = "Client"),
        CommonData(id = "34", name = "Emma Phillips", tagName = "Client"),
        CommonData(id = "35", name = "Oliver Lewis", tagName = "Client"),
        CommonData(id = "36", name = "Chloe Walker", tagName = "Client"),
        CommonData(id = "37", name = "Jack Hall", tagName = "Client"),
        CommonData(id = "38", name = "Amelia Allen", tagName = "Client"),
        CommonData(id = "39", name = "Noah Young", tagName = "Client"),
        CommonData(id = "40", name = "Ella King", tagName = "Client"),

        // Services (20)
        CommonData(id = "41", name = "Web Development", tagName = "Service"),
        CommonData(id = "42", name = "Digital Marketing", tagName = "Service"),
        CommonData(id = "43", name = "Cloud Computing", tagName = "Service"),
        CommonData(id = "44", name = "Cybersecurity", tagName = "Service"),
        CommonData(id = "45", name = "SEO Optimization", tagName = "Service"),
        CommonData(id = "46", name = "Mobile App Development", tagName = "Service"),
        CommonData(id = "47", name = "Data Analytics", tagName = "Service"),
        CommonData(id = "48", name = "IT Support", tagName = "Service"),
        CommonData(id = "49", name = "AI Solutions", tagName = "Service"),
        CommonData(id = "50", name = "E-commerce Solutions", tagName = "Service"),
        CommonData(id = "51", name = "Software Testing", tagName = "Service"),
        CommonData(id = "52", name = "Game Development", tagName = "Service"),
        CommonData(id = "53", name = "Blockchain Solutions", tagName = "Service"),
        CommonData(id = "54", name = "IoT Development", tagName = "Service"),
        CommonData(id = "55", name = "DevOps Consulting", tagName = "Service"),
        CommonData(id = "56", name = "HR Tech Solutions", tagName = "Service"),
        CommonData(id = "57", name = "UI/UX Design", tagName = "Service"),
        CommonData(id = "58", name = "AR/VR Solutions", tagName = "Service"),
        CommonData(id = "59", name = "Fintech Solutions", tagName = "Service"),
        CommonData(id = "60", name = "Big Data Analytics", tagName = "Service"),

        // Companies (20)
        CommonData(id = "61", name = "Acme Corp", tagName = "Company"),
        CommonData(id = "62", name = "XYZ Solutions", tagName = "Company"),
        CommonData(id = "63", name = "Innovate Tech", tagName = "Company"),
        CommonData(id = "64", name = "Future Enterprises", tagName = "Company"),
        CommonData(id = "65", name = "Global Systems", tagName = "Company"),
        CommonData(id = "66", name = "NexGen Software", tagName = "Company"),
        CommonData(id = "67", name = "BrightTech", tagName = "Company"),
        CommonData(id = "68", name = "Visionary AI", tagName = "Company"),
        CommonData(id = "69", name = "Cloud Innovators", tagName = "Company"),
        CommonData(id = "70", name = "Tech Pioneers", tagName = "Company"),
        CommonData(id = "71", name = "AI Labs", tagName = "Company"),
        CommonData(id = "72", name = "Quantum Inc.", tagName = "Company"),
        CommonData(id = "73", name = "ByteWorks", tagName = "Company"),
        CommonData(id = "74", name = "DeepMind Tech", tagName = "Company"),
        CommonData(id = "75", name = "NextWave Solutions", tagName = "Company"),
        CommonData(id = "76", name = "BlueSky Technologies", tagName = "Company"),
        CommonData(id = "77", name = "SmartEdge", tagName = "Company"),
        CommonData(id = "78", name = "Cloud Nexus", tagName = "Company"),
        CommonData(id = "79", name = "AI Revolution", tagName = "Company"),
        CommonData(id = "80", name = "FutureWave AI", tagName = "Company")
    )

    private val employeeList = allData.filter { it.tagName == "Employee" }
    private val clientList = allData.filter { it.tagName == "Client" }
    private var serviceList = allData.filter { it.tagName == "Service" }
    private var companyList = allData.filter { it.tagName == "Company" }



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            tabName = it.getString(ARG_PARAM1)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        // Inflate the layout for this fragment
        binding = FragmentCommonBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setOnClickListener()
        setUpAdapter()

    }

    private fun setOnClickListener(){
        binding.addDetails.setOnClickListener(this)
    }

    private fun setUpAdapter(){
        adapter= CommonAdapter()
        listener = object :CommonAdapter.CommunityListener{
            override fun btnDelete(bucketName:String, id:String) {
                if(!(bucketName == "null" || id == "null")){
                    when(tabName){
                        Constants.SERVICES_SEMI_CAPS -> {
                            // Remove item from serviceList
                            serviceList = serviceList.filter { it.id != id }
                            adapter.submitList(serviceList)
                        }
                        Constants.COMPANIES_SEMI_CAPS -> {
                            // Remove item from companyList
                            companyList = companyList.filter { it.id != id }
                            adapter.submitList(companyList)
                        }
                        Constants.EMPLOYEES_SEMI_CAPS->{

                        }
                        Constants.CLIENTS_SEMI_CAPS -> {

                        }
                        else -> {
                            // Handle other cases if needed
                        }
                    }
                }
            }

            override fun btnInfo(bucketName:String, id:String, name:String) {
                if(!(bucketName == "null" || id == "null" || name == "null")){
                    when(tabName){
                        Constants.EMPLOYEES_SEMI_CAPS->{
                            val bundle = Bundle().apply {
                                putString(Constants.USER_TYPE_SEMI_CAPS, tabName)
                                putString(Constants.USER_NAME, name)
                            }
                            findNavController().navigate(R.id.action_navigation_user_to_userInfoFragment,bundle)
                        }
                        Constants.CLIENTS_SEMI_CAPS -> {
                            val bundle = Bundle().apply {
                                putString(Constants.USER_TYPE_SEMI_CAPS, tabName)
                                putString(Constants.USER_NAME, name)
                            }
                            findNavController().navigate(R.id.action_navigation_user_to_userInfoFragment,bundle)
                        }
                        else -> {
                            // Handle other cases if needed
                        }
                    }
                }
            }

        }
        adapter.setData(tabName,listener)
        when(tabName){
            Constants.EMPLOYEES_SEMI_CAPS->{
                adapter.submitList(employeeList)
            }
            Constants.CLIENTS_SEMI_CAPS -> {
                adapter.submitList(clientList)
            }
            Constants.SERVICES_SEMI_CAPS -> {
                adapter.submitList(serviceList)
            }
            Constants.COMPANIES_SEMI_CAPS -> {
                adapter.submitList(companyList)
            }
            else ->{
            }
        }
        binding.rvUser.adapter=adapter
    }

    override fun onClick(v: View?) {
        when(v){
            binding.addDetails -> {
                val bundle = Bundle().apply {
                    putString(Constants.USER_TYPE_SEMI_CAPS, tabName) // Pass your data
                }
                findNavController().navigate(R.id.action_navigation_user_to_form_fragment, bundle)
            }
        }
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment CommonFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String) =
            CommonFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                }
            }
    }
}