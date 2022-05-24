package com.earth.testomania.technical.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.hilt.navigation.compose.hiltViewModel
import com.earth.testomania.core.helper.defaultTechQuiz
import com.earth.testomania.technical.domain.model.TechQuiz
import com.earth.testomania.technical.presentation.ui_parts.CreateQuizAnswerUI
import com.earth.testomania.technical.presentation.ui_parts.CreateQuizNavigationButtonUI
import com.earth.testomania.technical.presentation.ui_parts.CreateQuizUI
import com.earth.testomania.technical.presentation.ui_parts.OverallProgress
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.rememberPagerState
import com.ramcosta.composedestinations.annotation.DeepLink
import com.ramcosta.composedestinations.annotation.Destination
import kotlinx.coroutines.flow.collectLatest

@Destination(
    route = "home/technical_tests",
    deepLinks = [
        DeepLink(uriPattern = "testomania://home/technical_tests")
    ]
)
@Composable
fun TechnicalTestsScreen() {
    val viewModel: QuizViewModel = hiltViewModel()

    var data by remember {
        mutableStateOf<List<TechQuiz>>(emptyList())
    }

    LaunchedEffect(key1 = true) {
        viewModel.data.collectLatest {
            data = it
        }
    }

    CreateScreen(data)
}

@OptIn(ExperimentalPagerApi::class)
@Composable
private fun CreateScreen(techQuizList: List<TechQuiz>) {

    val pagerState = rememberPagerState()

    var currentProgress by remember {
        mutableStateOf(0)
    }

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collect { page ->
            currentProgress = page + 1
        }
    }

    ConstraintLayout(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .padding(all = 10.dp),
    ) {
        val (horizontalPager, navigation) = createRefs()

        OverallProgress(currentProgress, techQuizList.size)

        CreateQuizNavigationButtonUI(
            Modifier.constrainAs(navigation) {
                bottom.linkTo(parent.bottom)
            }
        )

        HorizontalPager(
            verticalAlignment = Alignment.Bottom,
            modifier = Modifier
                .wrapContentHeight()
                .fillMaxWidth()
                .constrainAs(horizontalPager) {
                    bottom.linkTo(navigation.top, margin = 1.dp)
                },
            count = techQuizList.size,
            state = pagerState,
        ) { page ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .padding(all = 10.dp)
            ) {
                CreateQuizUI(techQuizList[page])
                LazyColumn(
                    modifier = Modifier.wrapContentHeight()
                ) {
                    techQuizList[page].possibleAnswers.forEach { possibleAnswer ->
                        item {
                            CreateQuizAnswerUI(possibleAnswer)
                        }
                    }
                }
            }
        }


    }
}

@Preview
@Composable
private fun PreviewComposeUI() {
    CreateScreen(listOf(defaultTechQuiz()))
}