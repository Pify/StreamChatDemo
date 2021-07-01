package com.simpledeeds.streamchatdemo.ui.channel

import android.os.Bundle
import android.util.Log
import android.view.Gravity
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.simpledeeds.streamchatdemo.R
import com.simpledeeds.streamchatdemo.databinding.FragmentChannelBinding
import io.getstream.chat.android.client.ChatClient
import io.getstream.chat.android.client.models.Channel
import io.getstream.chat.android.client.models.Filters
import io.getstream.chat.android.client.models.User
import io.getstream.chat.android.client.models.name
import io.getstream.chat.android.livedata.ChatDomain
import io.getstream.chat.android.ui.avatar.AvatarView
import io.getstream.chat.android.ui.channel.list.header.viewmodel.ChannelListHeaderViewModel
import io.getstream.chat.android.ui.channel.list.header.viewmodel.bindView
import io.getstream.chat.android.ui.channel.list.viewmodel.ChannelListViewModel
import io.getstream.chat.android.ui.channel.list.viewmodel.bindView
import io.getstream.chat.android.ui.channel.list.viewmodel.factory.ChannelListViewModelFactory

class ChannelFragment : Fragment(R.layout.fragment_channel) {

    private val args: ChannelFragmentArgs by navArgs()

    private var _binding: FragmentChannelBinding? = null
    private val binding get() = _binding!!

    private val client = ChatClient.instance()
    private lateinit var user: User

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentChannelBinding.bind(view)

        setupUser()
        setupChannels()
        setupDrawer()

        binding.channelsView.setChannelDeleteClickListener { channel ->
            deleteChannel(channel)
        }

        binding.channelListHeaderView.setOnActionButtonClickListener {
            findNavController().navigate(R.id.action_channelFragment_to_usersFragment)
        }

        binding.channelsView.setChannelItemClickListener { channel ->
            val action = ChannelFragmentDirections.actionChannelFragmentToChatFragment(channel.cid)
            findNavController().navigate(action)
        }

        binding.channelListHeaderView.setOnUserAvatarClickListener {
            binding.drawerLayout.openDrawer(Gravity.START)
        }
    }

    private fun setupUser() {
        if(client.getCurrentUser() == null) {
            user = if (args.chatUser.firstName.contains("Rafif")) {
                User(
                    id = args.chatUser.username,
                    extraData = mutableMapOf(
                        "name" to args.chatUser.username,
                        "county" to "Malang",
                        "image" to "https://avatars.githubusercontent.com/u/57043130?v=4"
                    )
                )
            } else {
                User(
                    id = args.chatUser.username,
                    extraData = mutableMapOf(
                        "name" to args.chatUser.username
                    )
                )
            }
            val token = client.devToken(user.id)
            client.connectUser(
                user,
                token
            ).enqueue { result ->
                if (result.isSuccess) {
                    Log.d("ChannelFragmet", "Success connecting to user")
                } else {
                    Log.d("ChannelFragment", result.error().message.toString())
                }
            }
        }
    }

    private fun setupChannels() {

        //show filtered channel that current user involve
        val filters = Filters.and(
            Filters.eq("type", "messaging"),
            Filters.`in`("members", listOf(client.getCurrentUser()!!.id))
        )

        //provide necessary data to views
        val viewModelFactory = ChannelListViewModelFactory(
            filters,
            ChannelListViewModel.DEFAULT_SORT
        )
        val listViewModel: ChannelListViewModel by viewModels { viewModelFactory }
        val listHeaderViewModel: ChannelListHeaderViewModel by viewModels()

        listHeaderViewModel.bindView(binding.channelListHeaderView, viewLifecycleOwner)
        listViewModel.bindView(binding.channelsView, viewLifecycleOwner)
    }

    private fun setupDrawer() {
        binding.navigationView.setNavigationItemSelectedListener { menuItem ->
            if (menuItem.itemId == R.id.logout_menu) {
                logout()
            }
            false
        }
        val currentUser = client.getCurrentUser()!!
        val headerView = binding.navigationView.getHeaderView(0)
        val headerAvatar = headerView.findViewById<AvatarView>(R.id.avatarView)
        headerAvatar.setUserData(currentUser)
        val headerId = headerView.findViewById<TextView>(R.id.id_textView)
        headerId.text = currentUser.id
        val headerName = headerView.findViewById<TextView>(R.id.name_textView)
        headerName.text = currentUser.name
    }

    private fun deleteChannel(channel: Channel) {
        ChatDomain.instance().useCases.deleteChannel(channel.id).enqueue { result ->
            if (result.isSuccess) {
                showToast("Channel: ${channel.name} removed!")
            } else {
                Log.e("ChannelFragment", result.error().message.toString())
            }
        }
    }

    private fun logout() {
        val builder = AlertDialog.Builder(requireContext())
        builder.setPositiveButton("Yes") { _,_ ->
            client.disconnect()
            findNavController().navigate(R.id.action_channelFragment_to_loginFragment)
            showToast("Logged out successfully")
        }
        builder.apply {
            setNegativeButton("No") {_,_ -> }
            setTitle("Logout?")
            setMessage("Are you sure want to logout?")
            create().show()
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}