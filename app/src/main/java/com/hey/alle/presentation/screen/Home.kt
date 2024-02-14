package com.hey.alle.presentation.screen

import android.Manifest
import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.content.Context
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.rememberAsyncImagePainter
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.hey.alle.R
import com.hey.alle.domain.model.ScreenshotListState
import com.hey.alle.presentation.viewmodel.HomeViewModel
import kotlin.math.abs


@Preview
@Composable
fun HomeScreen(
    context: Context = LocalContext.current,
    viewModel: HomeViewModel = hiltViewModel()
){
    val state by viewModel.getScreenshotList.collectAsState()
    var isPermissionGranted by remember { mutableStateOf(false) }

    // Observing the permission here
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissionResults: Map<String, Boolean> ->
        isPermissionGranted = if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE){
            permissionResults[Manifest.permission.READ_MEDIA_IMAGES] == true
        }else{
            permissionResults[READ_EXTERNAL_STORAGE] != false
        }
    }

    // Checking if the permission is granted or Not
    if (isPermissionGranted){
        if(state.isLoading){
            // Showing lottie loading state
            ShowLoadingState()
            // Triggering media store api to fetch screenshot from the internal storage
            LaunchedEffect(Unit){
                viewModel.loadScreenShot(context)
            }
        }else{
            // Setting the Ui after fetching the list from internal storage
            HomeRootView(state)
        }
    }else{
        //Requesting the permission if it is not granted
        RequestPermission(launcher)
    }
}

@Composable
private fun RequestPermission(launcher: ManagedActivityResultLauncher<Array<String>, Map<String, @JvmSuppressWildcards Boolean>>) {
    LaunchedEffect(Unit){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            launcher.launch(arrayOf(Manifest.permission.READ_MEDIA_IMAGES))
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            launcher.launch(arrayOf(Manifest.permission.READ_MEDIA_IMAGES))
        } else {
            launcher.launch(arrayOf(READ_EXTERNAL_STORAGE))
        }
    }
}


@Composable
private fun ShowLoadingState(){
    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.image_loading))
    val progress by animateLottieCompositionAsState(composition, iterations = LottieConstants.IterateForever)
    LottieAnimation(
        modifier = Modifier.fillMaxSize(),
        alignment = Alignment.Center,
        composition = composition,
        progress = { progress })
}

@Composable
private fun HomeRootView(
    state: ScreenshotListState
) {
    val lazyState = rememberLazyListState()
    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var selectedItemIndex by remember { mutableIntStateOf(0) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        selectedUri?.let {
            Image(
                painter = rememberAsyncImagePainter(
                    model = Uri.parse(selectedUri.toString())
                ),
                contentDescription = "Parent Image View",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.FillWidth
            )
        }

       BoxWithConstraints(
           modifier = Modifier
           .fillMaxWidth()
           .padding(0.dp, 20.dp)
           .align(Alignment.BottomCenter)
       ) {
           val halfRowWidth = constraints.maxWidth / 2
           LazyRow(
               state = lazyState,
               verticalAlignment = Alignment.CenterVertically,
               horizontalArrangement = Arrangement.spacedBy(8.dp),
               contentPadding = PaddingValues(start = 16.dp, end = 16.dp),
           ){
               itemsIndexed(state.list){index, item ->

                   // This will calculate the center position of the item
                   val centerPosition by remember { // caching position for prevent recomposition
                       derivedStateOf {
                           val visibleinfo = lazyState.layoutInfo.visibleItemsInfo
                           if (visibleinfo.isEmpty()) -1
                           else {
                               val offset = (visibleinfo.last().index - visibleinfo.first().index) / 2
                               visibleinfo.first().index + offset
                           }
                       }
                   }

                   // This will scale the item while scrolling the item
                   val scale by remember {
                       derivedStateOf {
                           val currentItemInfo = lazyState.layoutInfo.visibleItemsInfo.firstOrNull {
                               it.index == index
                           } ?: return@derivedStateOf 0.8f
                           val itemHalfSize = currentItemInfo.size / 2
                           1f - minOf(1f, abs(currentItemInfo.offset + itemHalfSize - halfRowWidth).toFloat() / halfRowWidth) * 0.8f
                       }
                   }


                   // This will check whether the center item position is equal to index
                   // If yes it will pick the same uri from the list and update to the state
                   // which will eventually display to the parent big image
                   if(index == centerPosition){
                       selectedUri = state.list.get(index)
                   }


                   // This is the child item view
                   Child(
                       item,
                       modifier = Modifier
                           .size(60.dp, 60.dp)
                           .graphicsLayer {
                               scaleX = scale
                               scaleY = scale
                               alpha = scale
                           },
                       imageModifier = Modifier.requiredWidth(60.dp)
                   )
               }
           }
       }
    }
}


@Composable
fun Child(item: Uri, modifier: Modifier, imageModifier: Modifier,) {
    Card(
        shape = RoundedCornerShape(8.dp),
        modifier = modifier,
    ) {
        Image(
            painter = rememberAsyncImagePainter(
                model = Uri.parse(item.toString())
            ),
            contentDescription = "Selected Content Uri",
            modifier = imageModifier,
            contentScale = ContentScale.Crop
        )
    }
}



