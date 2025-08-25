<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import dayjs from 'dayjs'
import {
  getSystemOverviewApi,
  getUserActivityApi,
  getUserActivityTrendApi,
  getTopKnowledgeScoreApi,
  getKnowledgeDistributionApi,
  getScoreTrendApi,
  getInteractionStatApi,
  getAiInteractionStatApi
} from '@/api/admin'

// ====== ECharts 基础注册（与 <vue-echarts> 配合）======
import * as echarts from 'echarts/core'
import { BarChart, LineChart, PieChart } from 'echarts/charts'
import { GridComponent, TooltipComponent, LegendComponent, TitleComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
echarts.use([BarChart, LineChart, PieChart, GridComponent, TooltipComponent, LegendComponent, TitleComponent, CanvasRenderer])

// 自定义深色主题（可选）
echarts.registerTheme('dashboardDark', {
  backgroundColor: 'transparent',
  textStyle: { color: '#cfd8e3' },
  legend: { textStyle: { color: '#cfd8e3' } },
  tooltip: { backgroundColor: 'rgba(17,24,39,.92)', borderColor: '#334155' },
  grid: { containLabel: true },
  valueAxis: {
    splitLine: { lineStyle: { color: 'rgba(148,163,184,.18)' } },
    axisLine: { lineStyle: { color: '#94a3b8' } },
    axisLabel: { color: '#cfd8e3' }
  },
  categoryAxis: {
    axisLine: { lineStyle: { color: '#94a3b8' } },
    axisLabel: { color: '#cfd8e3' },
    splitLine: { show: false }
  }
})

// =====================================================
// =============== 顶部标签：一次只显示一张图 ===========
// =====================================================
const tabs = [
  { key: 'score', label: '作业平均成绩走向', icon: 'fas fa-book' },
  { key: 'qna', label: '学生提问情况', icon: 'fas fa-question-circle' },
  { key: 'ai', label: 'AI 交互活跃度', icon: 'fas fa-robot' },
  { key: 'activity', label: '用户活跃度趋势', icon: 'fas fa-users' },
  { key: 'knowledge', label: '知识点分析', icon: 'fas fa-signal' } // 合并：Top5 + 分布
]
const activeTab = ref('score')
const show = (key) => activeTab.value === key
const onTabClick = (key) => {
  activeTab.value = key
  // 切换后触发重算，避免尺寸问题
  requestAnimationFrame(() => window.dispatchEvent(new Event('resize')))
}

// =====================================================
// ================== 状态 & 工具函数 ==================
// =====================================================
const loading = ref({ overview: false, activity: false })
const lastRefreshAt = ref('—')

// KPI（如果页面其它地方要用，可保留）
const totalUsers = ref(0)
const activeUsers = ref(0)
const totalResources = ref(0)
const storageBytes = ref(0)
const newUsersToday = ref(0)

const userActivity = ref({
  todayActive: 0,
  todayNewUsers: 0,
  avgSessionTime: 0,
  totalLogins: 0,
  uniqueLogins: 0
})

const formatBytes = (bytes) => {
  if (!bytes) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB', 'TB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i]
}
const formatUptime = (seconds) => {
  const s = Number(seconds || 0)
  const days = Math.floor(s / 86400)
  const hours = Math.floor((s % 86400) / 3600)
  return `${days}天 ${hours}小时`
}
const activeRateText = computed(() => {
  const total = Number(totalUsers.value || 0)
  const active = Number(activeUsers.value || 0)
  if (total <= 0) return '--'
  return `${Math.round((active / total) * 100)}%`
})

// =====================================================
// ====================== 数据拉取 ======================
// =====================================================
const userActivityTrend = ref([]) // 活跃趋势
const barOption = ref({})         // 知识点 Top5
const pieOption = ref({})         // 知识点分布
const pieTitle = ref('知识点掌握分布') // 分布标题

const loadSystemOverview = async () => {
  loading.value.overview = true
  try {
    const res = await getSystemOverviewApi()
    if (res?.code === 1) {
      const d = res.data || {}
      totalUsers.value = d.totalUsers || 0
      activeUsers.value = d.activeUsers || 0
      totalResources.value = d.totalResources || 0
      storageBytes.value = d.totalStorage || 0
      newUsersToday.value = d.newUsersToday || 0
    } else {
      ElMessage.error(res?.msg || '获取系统概览失败')
    }
  } catch (e) {
    console.error(e)
    ElMessage.error('获取系统概览失败')
  } finally {
    loading.value.overview = false
  }
}

const loadUserActivity = async () => {
  loading.value.activity = true
  try {
    const res = await getUserActivityApi()
    if (res?.code === 1) {
      userActivity.value = {
        todayActive: Number(res.data?.todayActive || 0),
        todayNewUsers: Number(res.data?.todayNewUsers || 0),
        avgSessionTime: Number(res.data?.avgSessionTime || 0),
        totalLogins: Number(res.data?.totalLogins || 0),
        uniqueLogins: Number(res.data?.uniqueLogins || 0)
      }
    } else {
      ElMessage.error(res?.msg || '获取用户活跃度失败')
    }
  } catch (e) {
    console.error(e)
    ElMessage.error('获取用户活跃度失败')
  } finally {
    loading.value.activity = false
  }
}

const loadUserActivityTrend = async () => {
  try {
    const res = await getUserActivityTrendApi({
      startDate: dayjs().subtract(6, 'day').format('YYYY-MM-DD'),
      endDate: dayjs().format('YYYY-MM-DD'),
      granularity: 'day'
    })
    if (res?.code === 1) {
      userActivityTrend.value = Array.isArray(res.data) ? res.data : []
    } else {
      console.warn('获取用户活跃度趋势失败:', res?.msg)
      userActivityTrend.value = []
    }
  } catch (e) {
    console.error(e)
    userActivityTrend.value = []
  }
}

// Top 知识点条形图
// const loadBarChart = async () => {
//   const res = await getTopKnowledgeScoreApi()
//   const raw = Array.isArray(res?.data) ? res.data : []
//   const data = raw.filter(i => i?.knowledge)
//   const colors = ['#5470C6', '#91CC75', '#EE6666', '#FAC858', '#73C0DE']

//   if (data.length === 0) {
//     barOption.value = {
//       title: { text: '暂无数据', left: 'center', top: 'middle', textStyle: { color: '#8b9fb6' } }
//     }
//     pieTitle.value = '知识点掌握分布'
//     pieOption.value = { series: [] }
//     return
//   }

//   barOption.value = {
//     tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' }, formatter: '{b}: {c}%' },
//     grid: { left: 10, top: 10, bottom: 10, right: 10, containLabel: true },
//     xAxis: { type: 'value', max: 100, axisLabel: { formatter: '{value}%' } },
//     yAxis: {
//       type: 'category',
//       data: data.map(i => i.knowledge),
//       inverse: true,
//       axisLabel: { overflow: 'truncate', width: 160 }
//     },
//     series: [{
//       type: 'bar',
//       name: '得分率',
//       data: data.map(i => Number(i.scoreRate) || 0),
//       // label: { show: true, position: 'right', formatter: '{c}%' },
//       itemStyle: { color: p => colors[p.dataIndex % colors.length] }
//     }]
//   }

//   // 默认选择第一项联动饼图
//   await loadPieChart(data[0].knowledge)
// }

const loadBarChart = async () => {
  const res = await getTopKnowledgeScoreApi()
  const raw = Array.isArray(res?.data) ? res.data : []
  const data = raw.filter(i => i?.knowledge)
  const colors = ['#5470C6', '#91CC75', '#EE6666', '#FAC858', '#73C0DE']

  if (data.length === 0) {
    barOption.value = {
      title: { 
        text: '暂无数据', 
        left: 'center', 
        top: 'middle', 
        textStyle: { color: '#8b9fb6', fontSize: 16 }
      }
    }
    pieTitle.value = '知识点掌握分布'
    pieOption.value = { series: [] }
    return
  }

  barOption.value = {
    tooltip: { 
      trigger: 'axis', 
      axisPointer: { type: 'shadow' },
      formatter: '{b}: {c}%', 
      backgroundColor: 'rgba(0, 0, 0, 0.7)', // 提示框背景色
      textStyle: { color: '#fff' } // 提示框字体颜色
    },
    grid: { 
      left: 10, 
      top: 10, 
      bottom: 10, 
      right: 10, 
      containLabel: true 
    },
    xAxis: { 
      type: 'value', 
      max: 100, 
      axisLabel: { formatter: '{value}%' },
      axisLine: { lineStyle: { color: '#000' } } // x轴线颜色
    },
    yAxis: {
      type: 'category',
      data: data.map(i => i.knowledge),
      inverse: true,
      axisLabel: { 
        overflow: 'truncate', 
        width: 160,
        fontSize: 16, // y轴标签字体大小
        fontWeight: '300', // y轴标签字体粗细
        color: '#000' // y轴标签颜色
      },
      axisLine: { lineStyle: { color: '#000' } } // y轴线颜色
    },
    series: [{
      type: 'bar',
      name: '得分率',
      data: data.map(i => Number(i.scoreRate) || 0),
      itemStyle: { color: (p) => colors[p.dataIndex % colors.length] },
      label: { 
        show: true, 
        position: 'right', 
        formatter: '{c}%', 
        fontSize: 14, // 数据标签字体大小
        color: '#000' // 数据标签字体颜色
      }
    }]
  }

  // 默认选择第一项联动饼图
  await loadPieChart(data[0].knowledge)
}

// 分布饼图
// const loadPieChart = async (knowledgeName) => {
//   const res = await getKnowledgeDistributionApi({
//     knowledgeName: knowledgeName
//   })
//   const dist = res.data // 格式：[{ label: '掌握好', count: 15 }, ...]

//   pieTitle.value = `${knowledgeName} 知识点情况掌握分布`
//   pieOption.value = {
//     tooltip: {
//       trigger: 'item',
//       formatter: '{b}: {c}人 ({d}%)'
//     },
//     legend: {
//       bottom: 0,
//       data: dist.map(item => item.label)
//     },
//     series: [{
//       type: 'pie',
//       radius: '60%',
//       data: dist.map(item => {
//         let color = '#ccc'
//         if (item.label === '优秀') color = '#91cc75'
//         else if (item.label === '良好') color = '#fac858'
//         else if (item.label === '一般') color = '#ee6666'
//         return {
//           name: item.label,
//           value: item.count,
//           itemStyle: { color }
//         }
//       }),
//       label: {
//         formatter: '{b}:{c}题({d}%)'
//       }
//     }]
//   }
// }

const loadPieChart = async (knowledgeName) => {
  const res = await getKnowledgeDistributionApi({
    knowledgeName: knowledgeName
  })
  const dist = res.data // 格式：[{ label: '掌握好', count: 15 }, ...]

  pieTitle.value = `${knowledgeName} 知识点情况掌握分布`
  pieOption.value = {
    tooltip: {
      trigger: 'item',
      formatter: '{b}: {c}人 ({d}%)',
      backgroundColor: 'rgba(0, 0, 0, 0.7)', // 提示框背景色
      textStyle: { color: '#fff' } // 提示框字体颜色
    },
    legend: {
      bottom: 0,
      data: dist.map(item => item.label),
      textStyle: {
        color: '#000', // 图例字体颜色
        fontSize: 14 // 图例字体大小
      }
    },
    series: [{
      type: 'pie',
      radius: '60%',
      data: dist.map(item => {
        let color = '#ccc'
        if (item.label === '优秀') color = '#91cc75'  // 绿色
        else if (item.label === '良好') color = '#fac858'  // 黄色
        else if (item.label === '一般') color = '#ee6666'  // 红色
        return {
          name: item.label,
          value: item.count,
          itemStyle: { color }
        }
      }),
      label: {
        formatter: '{b}:{c}题({d}%)',
        fontSize: 14,  // 标签字体大小
        color: '#000'  // 标签字体颜色
      },
      labelLine: {
        lineStyle: {
          color: '#000'  // 标签线颜色
        }
      }
    }]
  }
}


// 条形图点击联动饼图
const onBarClick = (params) => {
  if (params?.name) loadPieChart(params.name)
}

// =====================================================
// ==================== 图表配置 ========================
// =====================================================

const homeworkScoreOption = ref({})

// const loadScoreTrend = async () => {
//   try {
//     const res = await getScoreTrendApi()
//     if (res?.code === 1) {
//       const data = res.data || []

//       homeworkScoreOption.value = {
//         tooltip: { trigger: 'axis' },
//         legend: { data: ['平均分', '提交率'] },
//         xAxis: { type: 'category', data: data.map(d => d.title || `作业${d.homeworkId}`) },
//         yAxis: [
//           { type: 'value', name: '平均分', min: 0, max: 100 },
//           { type: 'value', name: '提交率', min: 0, max: 100, axisLabel: { formatter: '{value}%' } }
//         ],
//         series: [
//           { name: '平均分', type: 'bar', data: data.map(d => d.avgScore || 0), label: { show: true, position: 'top' }, itemStyle: { color: '#409EFF' } },
//           { name: '提交率', type: 'line', yAxisIndex: 1, smooth: true, data: data.map(d => Math.round((d.submitRate || 0) * 100)), label: { show: true, formatter: '{c}%' }, itemStyle: { color: '#67C23A' } }
//         ]
//       }
//     }
//   } catch (e) {
//     console.error('获取作业成绩趋势失败:', e)
//   }
// }


const loadScoreTrend = async () => {
  try {
    const res = await getScoreTrendApi()
    if (res?.code === 1) {
      const data = res.data || []
      homeworkScoreOption.value = {
        tooltip: {
          trigger: 'axis',
          backgroundColor: 'rgba(0, 0, 0, 0.7)', // 提示框背景色
          textStyle: {
            color: '#000' // 提示框字体颜色
          }
        },
        legend: {
          data: ['平均分', '提交率'],

          textStyle: {
            color: '#000', // 图例字体颜色
            fontSize: 14
          }
        },
        xAxis: {
          type: 'category',

          data: data.map(d => d.title || `作业${d.homeworkId}`),
          axisLabel: {
            fontSize: 16,  // 横坐标字体大小
            fontWeight: '300',  // 横坐标字体加粗
            color: '#000'  // 横坐标字体颜色
          },
          axisLine: {
            lineStyle: {
              color: '#000' // 横坐标轴线的颜色
            }
          }
        },
        yAxis: [
          {
            type: 'value',
            name: '平均分',
            min: 0,
            nameTextStyle: {
              color: '#000',  // 设置“提交率”颜色为绿色
              fontSize: 15,      // 设置字体大小为18px
              fontWeight: 'bold' // 设置字体加粗
            },
            max: 100,
            axisLabel: {
              fontSize: 16,  // 纵坐标字体大小
              fontWeight: '200',  // 纵坐标字体加粗
              color: '#000' // 纵坐标字体颜色
            },
            axisLine: {
              lineStyle: {
                color: '#000' // 纵坐标轴线的颜色
              }
            }
          },
          {
            type: 'value',
            name: '提交率',
            min: 0,
            nameTextStyle: {
              color: '#000',  // 设置“提交率”颜色为绿色
              fontSize: 15,      // 设置字体大小为18px
              fontWeight: 'bold' // 设置字体加粗
            },
            max: 100,
            axisLabel: {
              fontSize: 16,  // 纵坐标字体大小
              fontWeight: '200',  // 纵坐标字体加粗
              color: '#000', // 纵坐标字体颜色
              formatter: '{value}%'  // 提交率加百分号
            },
            axisLine: {
              lineStyle: {
                color: '#000' // 纵坐标字体颜色
              }
            }
          }
        ],
        series: [
          {
            name: '平均分',
            type: 'bar',
            data: data.map(d => d.avgScore || 0),
            label: {
              show: true,
              position: 'top',
              fontSize: 14,  // 数据标签字体大小
              color: '#000'
            },
            itemStyle: {
              color: '#409EFF'  // 设置柱状图颜色
            }
          },
          {
            name: '提交率',
            type: 'line',
            yAxisIndex: 1,
            smooth: true,
            data: data.map(d => Math.round((d.submitRate || 0) * 100)),
            label: {
              show: true,
              formatter: '{c}%',
              fontSize: 14,  // 数据标签字体大小
              color: '#67C23A'
            },
            itemStyle: {
              color: '#67C23A'  // 设置线条颜色
            }
          }
        ]
      }
    }
  } catch (e) {
    console.error('获取作业成绩趋势失败:', e)
  }
}


const studentQuestionOption = ref({})

// const loadInteractionStat = async () => {
//   try {
//     const res = await getInteractionStatApi()
//     if (res?.code === 1) {
//       const data = res.data || []

//       studentQuestionOption.value = {
//         tooltip: { trigger: 'axis' },
//         legend: { data: ['已答', '未答'], bottom: 0 },
//         xAxis: { type: 'category', data: data.map(d => dayjs(d.d).format('MM-DD')) },
//         yAxis: { type: 'value', name: '提问数' },
//         series: [
//           { name: '已答', type: 'bar', stack: 'total', data: data.map(d => d.answered || 0), itemStyle: { color: '#409EFF' } },
//           { name: '未答', type: 'bar', stack: 'total', data: data.map(d => d.unanswered || 0), itemStyle: { color: '#F56C6C' } }
//         ]
//       }
//     }
//   } catch (e) {
//     console.error('获取学生提问统计失败:', e)
//   }
// }
const loadInteractionStat = async () => {
  try {
    const res = await getInteractionStatApi()
    if (res?.code === 1) {
      const data = res.data || []

      studentQuestionOption.value = {
        tooltip: { 
          trigger: 'axis',
          backgroundColor: 'rgba(0, 0, 0, 0.7)', // 提示框背景色
          textStyle: {
            color: '#000' // 提示框字体颜色
          }
        },
        legend: { 
          data: ['已答', '未答'], 
          bottom: 0,
          textStyle: {
            color: '#000', // 图例字体颜色
            fontSize: 14 // 图例字体大小
          }
        },
        xAxis: {
          type: 'category',
          data: data.map(d => dayjs(d.d).format('MM-DD')),
          axisLabel: {
            fontSize: 16,  // 横坐标字体大小
            fontWeight: '300',  // 横坐标字体加粗
            color: '#000'  // 横坐标字体颜色
          },
          axisLine: {
            lineStyle: {
              color: '#000' // 横坐标轴线的颜色
            }
          }
        },
        yAxis: {
          type: 'value',
          name: '提问数',
          nameTextStyle: {
            color: '#000',  // 纵坐标名称颜色
            fontSize: 15,    // 纵坐标名称字体大小
            fontWeight: 'bold' // 纵坐标名称字体加粗
          },
          axisLabel: {
            fontSize: 16,  // 纵坐标字体大小
            fontWeight: '200',  // 纵坐标字体加粗
            color: '#000' // 纵坐标字体颜色
          },
          axisLine: {
            lineStyle: {
              color: '#000' // 纵坐标轴线的颜色
            }
          }
        },
        series: [
          { 
            name: '已答', 
            type: 'bar', 
            stack: 'total', 
            data: data.map(d => d.answered || 0), 
            itemStyle: { color: '#409EFF' } 
          },
          { 
            name: '未答', 
            type: 'bar', 
            stack: 'total', 
            data: data.map(d => d.unanswered || 0), 
            itemStyle: { color: '#F56C6C' } 
          }
        ]
      }
    }
  } catch (e) {
    console.error('获取学生提问统计失败:', e)
  }
}



const aiTrendOption = ref({})

// const loadAiTrend = async () => {
//   try {
//     const res = await getAiInteractionStatApi()
//     if (res?.code === 1) {
//       const data = res.data || []

//       aiTrendOption.value = {
//         tooltip: { trigger: 'axis' },
//         xAxis: { type: 'category', data: data.map(d => dayjs(d.d).format('MM-DD')), boundaryGap: false },
//         yAxis: { type: 'value', name: '提问次数',minInterval: 1 },
//         series: [{
//           name: 'AI提问次数',
//           type: 'line',
//           data: data.map(d => d.cnt || 0),
//           smooth: true,
//           areaStyle: { opacity: 0.25 },
//           itemStyle: { color: '#a78bfa' }
//         }]
//       }
//     }
//   } catch (e) {
//     console.error('获取AI交互趋势失败:', e)
//   }
// }



// ④ 用户活跃趋势（近7天折线）

const loadAiTrend = async () => {
  try {
    const res = await getAiInteractionStatApi()
    if (res?.code === 1) {
      const data = res.data || []

      aiTrendOption.value = {
        tooltip: { 
          trigger: 'axis',
          backgroundColor: 'rgba(0, 0, 0, 0.7)', // 提示框背景色
          textStyle: {
            color: '#000' // 提示框字体颜色
          }
        },
        legend: {
          data: ['AI提问次数'],
          textStyle: {
            color: '#000', // 图例字体颜色
            fontSize: 14 // 图例字体大小
          },
          bottom: 0
        },
        xAxis: { 
          type: 'category',
          data: data.map(d => dayjs(d.d).format('MM-DD')),
          boundaryGap: false,
          axisLabel: {
            fontSize: 16,  // 横坐标字体大小
            fontWeight: '300',  // 横坐标字体加粗
            color: '#000'  // 横坐标字体颜色
          },
          axisLine: {
            lineStyle: {
              color: '#000' // 横坐标轴线的颜色
            }
          }
        },
        yAxis: { 
          type: 'value', 
          name: '提问次数',
          nameTextStyle: {
            color: '#000',  // 纵坐标名称颜色
            fontSize: 15,    // 纵坐标名称字体大小
            fontWeight: 'bold' // 纵坐标名称字体加粗
          },
          minInterval: 1,
          axisLabel: {
            fontSize: 16,  // 纵坐标字体大小
            fontWeight: '200',  // 纵坐标字体加粗
            color: '#000' // 纵坐标字体颜色
          },
          axisLine: {
            lineStyle: {
              color: '#000' // 纵坐标轴线的颜色
            }
          }
        },
        series: [{
          name: 'AI提问次数',
          type: 'line',
          data: data.map(d => d.cnt || 0),
          smooth: true,
          areaStyle: { opacity: 0.25 },
          itemStyle: { color: '#a78bfa' },
          label: {
            show: true,
            fontSize: 14,  // 数据标签字体大小
            color: '#000'  // 数据标签字体颜色
          }
        }]
      }
    }
  } catch (e) {
    console.error('获取AI交互趋势失败:', e)
  }
}


// const userActivityTrendOption = computed(() => {
//   const trend = userActivityTrend.value || []
//   const dates = trend.map(it => it?.date ? dayjs(it.date).format('MM-DD') : '').filter(Boolean)
//   const counts = trend.map(it => Number(it?.activeUserCount || 0))

//   const mk = (ds, cs) => ({
//     tooltip: { trigger: 'axis' },
//     xAxis: { type: 'category', data: ds, boundaryGap: false },
//     yAxis: { type: 'value', name: '活跃用户数' },
//     series: [{ name: '活跃用户数', type: 'line', data: cs, smooth: true, areaStyle: { opacity: 0.25 }, itemStyle: { color: '#409eff' } }]
//   })

//   if (dates.length === 0) {
//     const ds = [], cs = []
//     for (let i = 6; i >= 0; i--) { ds.push(dayjs().subtract(i, 'day').format('MM-DD')); cs.push(0) }
//     return mk(ds, cs)
//   }
//   return mk(dates, counts)
// })

// =====================================================
// ================= 刷新/初始化入口 ====================
// =====================================================

const userActivityTrendOption = computed(() => {
  const trend = userActivityTrend.value || []
  const dates = trend.map(it => it?.date ? dayjs(it.date).format('MM-DD') : '').filter(Boolean)
  const counts = trend.map(it => Number(it?.activeUserCount || 0))

  const mk = (ds, cs) => ({
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'rgba(0, 0, 0, 0.7)', // 提示框背景色
      textStyle: {
        color: '#000' // 提示框字体颜色
      }
    },
    legend: {
      data: ['活跃用户数'],
      textStyle: {
        color: '#000', // 图例字体颜色
        fontSize: 14 // 图例字体大小
      },
      bottom: 0
    },
    xAxis: {
      type: 'category',
      data: ds,
      boundaryGap: false,
      axisLabel: {
        fontSize: 16,  // 横坐标字体大小
        fontWeight: '300',  // 横坐标字体加粗
        color: '#000'  // 横坐标字体颜色
      },
      axisLine: {
        lineStyle: {
          color: '#000' // 横坐标轴线的颜色
        }
      }
    },
    yAxis: {
      type: 'value',
      name: '活跃用户数',
      nameTextStyle: {
        color: '#000',  // 纵坐标名称颜色
        fontSize: 15,    // 纵坐标名称字体大小
        fontWeight: 'bold' // 纵坐标名称字体加粗
      },
      axisLabel: {
        fontSize: 16,  // 纵坐标字体大小
        fontWeight: '200',  // 纵坐标字体加粗
        color: '#000' // 纵坐标字体颜色
      },
      axisLine: {
        lineStyle: {
          color: '#000' // 纵坐标轴线的颜色
        }
      }
    },
    series: [{
      name: '活跃用户数',
      type: 'line',
      data: cs,
      smooth: true,
      areaStyle: { opacity: 0.25 },
      itemStyle: { color: '#409eff' },
      label: {
        show: true,
        fontSize: 14,  // 数据标签字体大小
        color: '#000'  // 数据标签字体颜色
      }
    }]
  })

  if (dates.length === 0) {
    const ds = [], cs = []
    for (let i = 6; i >= 0; i--) { 
      ds.push(dayjs().subtract(i, 'day').format('MM-DD')) 
      cs.push(0) 
    }
    return mk(ds, cs)
  }
  return mk(dates, counts)
})


const loadAllData = async () => {
  await Promise.all([
    loadSystemOverview(),
    loadUserActivity(),
    loadUserActivityTrend(),
    loadBarChart(),
    loadScoreTrend(),        // 作业成绩
    loadInteractionStat(),   // 学生提问
    loadAiTrend()            // AI交互
  ])
  lastRefreshAt.value = dayjs().format('HH:mm:ss')
}
const refreshData = () => {
  loadAllData()
  ElMessage.success('数据已刷新')
}

onMounted(() => {
  loadAllData()
})


</script>


<template>
  <div class="page-wrap">
    <!-- 头部：标题 + 刷新 -->
    <div class="page-header">
      <h2 class="title">
        <i class="fas fa-chart-line"></i> 教学实训 · 可视化
      </h2>
      <div class="actions">
        <span class="muted">上次刷新：{{ lastRefreshAt }}</span>
        <el-button text type="success" :loading="loading?.overview" @click="refreshData">
          <i class="fas fa-rotate"></i>&nbsp;刷新
        </el-button>
      </div>
    </div>

    <!-- 顶部切换按钮（按你的顺序） -->
    <div class="view-tabs">
      <button v-for="t in tabs" :key="t.key" class="view-tab" :class="{ active: activeTab === t.key }"
        @click="onTabClick(t.key)">
        <i :class="t.icon"></i>
        <span>{{ t.label }}</span>
      </button>
    </div>

    <!-- 内容面板：一次只显示一个图 -->
    <section class="view-panel glass">
      <!-- 1 作业平均成绩走向 -->
      <div v-if="show('score')" class="panel-body">
        <div class="chart-title"><i class="fas fa-book"></i> 作业平均成绩走向</div>
        <vue-echarts :option="homeworkScoreOption" theme="dashboardDark" :autoresize="true"
          style="width:100%; height:calc(100% - 36px);" />
      </div>

      <!-- 2 学生提问情况 -->
      <div v-else-if="show('qna')" class="panel-body">
        <div class="chart-title"><i class="fas fa-question-circle"></i> 学生提问情况</div>
        <vue-echarts :option="studentQuestionOption" theme="dashboardDark" :autoresize="true"
          style="width:100%; height:calc(100% - 36px);" />
      </div>

      <!-- 3 AI 交互活跃度 -->
      <div v-else-if="show('ai')" class="panel-body">
        <div class="chart-title"><i class="fas fa-robot"></i> AI 交互活跃度（近7天）</div>
        <vue-echarts :option="aiTrendOption" theme="dashboardDark" :autoresize="true"
          style="width:100%; height:calc(100% - 36px);" />
      </div>

      <!-- 4 用户活跃度趋势 -->
      <div v-else-if="show('activity')" class="panel-body">
        <div class="chart-title"><i class="fas fa-users"></i> 用户活跃度趋势（近7天）</div>
        <vue-echarts :option="userActivityTrendOption" theme="dashboardDark" :autoresize="true"
          style="width:100%; height:calc(100% - 36px);" />
      </div>

      <!-- 5 知识点分析（合并：Top5 + 分布） -->
      <div v-else-if="show('knowledge')" class="panel-body">
        <div class="chart-title"><i class="fas fa-signal"></i> 知识点分析（Top5 + 分布）</div>
        <div class="knowledge-two">
          <!-- 左：Top5 -->
          <vue-echarts :option="barOption" theme="dashboardDark" :autoresize="true" @click="onBarClick" />
          <!-- 右：分布 -->
          <vue-echarts :option="pieOption" theme="dashboardDark" :autoresize="true" />
        </div>
        <div class="sub-title">{{ pieTitle }}</div>
      </div>
    </section>
  </div>
</template>


<!-- <style scoped>
@import url('https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.0.0/css/all.min.css');

/* 页面整体 */
.page-wrap {
  padding: 20px;
  min-height: 100vh;
  /* background: linear-gradient(135deg, #1e293b 0%, #0f172a 100%); */
  color: #e2e8f0;
  font-family: 'Segoe UI', sans-serif;
}

/* 头部区域 */
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.title {
  font-size: 22px;
  font-weight: 700;
  color: #f8fafc;
}

.actions {
  display: flex;
  gap: 10px;
  align-items: center;
}

.muted {
  color: #94a3b8;
  font-size: 14px;
}

/* 标签按钮条 */
.view-tabs {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  margin: 8px 0 12px;
}

.view-tab {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 14px;
  border-radius: 10px;
  cursor: pointer;
  background: rgba(255, 255, 255, 0.06);
  border: 1px solid rgba(255, 255, 255, 0.1);
  color: #cfe3ff;
  font-size: 14px;
  transition: all 0.2s;
}

.view-tab:hover {
  transform: translateY(-1px);
  box-shadow: 0 6px 18px rgba(0, 0, 0, 0.25);
}

.view-tab.active {
  color: #fff;
  background: rgba(64, 158, 255, 0.25);
  border-color: #409eff;
}

/* 内容区整体卡片 */
.view-panel {
  height: 70vh;
  /* 页面中占6成高，可根据需要调整 */
  padding: 14px;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.06);
  border: 1px solid rgba(255, 255, 255, 0.1);
  box-shadow: 0 6px 24px rgba(0, 0, 0, 0.35);
}

/* 单个图容器 */
.panel-body {
  background-color: rgba(255, 255, 255, 0.15);
  height: 100%;
  display: flex;
  flex-direction: column;
  padding-bottom: 5px;
}

.chart-title {
  font-size: 18px;
  font-weight: 600;
  margin-bottom: 10px;
  display: flex;
  margin-top: 5px;
  justify-content: center;
  align-items: center;
  gap: 8px;
  color: black;
}

.sub-title {
  margin-top: 6px;
  color: black;
  font-size: 15px;
  margin-left: 800px;
}

/* 知识点分析合并布局 */
.knowledge-two {
  display: grid;
  grid-template-columns: 7fr 5fr;
  /* 左右比例可调整 */
  gap: 14px;
  width: 100%;
  height: calc(100% - 36px);
  /* 扣掉标题高度 */
}

.knowledge-two>* {
  width: 100%;
  height: 100%;
}

/* 响应式适配 */
@media (max-width: 1024px) {
  .view-panel {
    height: 58vh;
  }

  .knowledge-two {
    grid-template-columns: 1fr;
    /* 小屏上下排列 */
  }
}
</style> -->

<style scoped>
@import url('https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.0.0/css/all.min.css');

/* 页面整体 */
.page-wrap {
  padding: 20px;
  min-height: 100vh;
  background: #f9f9f9;
  color: #333;
  font-family: 'Segoe UI', sans-serif;
}

/* 头部区域 */
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.title {
  font-size: 22px;
  font-weight: 700;
  color: #333;
}

.actions {
  display: flex;
  gap: 10px;
  align-items: center;
}

.muted {
  color: #777;
  font-size: 14px;
}

/* 标签按钮条 */
.view-tabs {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  margin: 8px 0 12px;
}

.view-tab {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 14px;
  border-radius: 10px;
  cursor: pointer;
  background: rgba(0, 0, 0, 0.1);
  border: 1px solid rgba(0, 0, 0, 0.2);
  color: #333;
  font-size: 14px;
  transition: all 0.2s;
}

.view-tab:hover {
  transform: translateY(-1px);
  box-shadow: 0 6px 18px rgba(0, 0, 0, 0.25);
}

.view-tab.active {
  color: #333;
  background: rgba(0, 0, 0, 0.15);
  border-color: #333;
}

/* 内容区整体卡片 */
.view-panel {
  height: 70vh;
  padding: 14px;
  border-radius: 12px;
  background: #fff;
  border: 1px solid #e0e0e0;
  box-shadow: 0 6px 24px rgba(0, 0, 0, 0.15);
}

/* 单个图容器 */
.panel-body {
  background-color: rgba(0, 0, 0, 0.05);
  height: 100%;
  display: flex;
  flex-direction: column;
  padding-bottom: 5px;
}

.chart-title {
  font-size: 18px;
  font-weight: 600;
  margin-bottom: 10px;
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 8px;
  color: #333;
}

.sub-title {
  margin-top: 6px;
  color: #333;
  font-size: 15px;
  margin-left: 730px;
}

/* 知识点分析合并布局 */
.knowledge-two {
  display: grid;
  grid-template-columns: 7fr 5fr;
  gap: 14px;
  width: 100%;
  height: calc(100% - 36px);
}

.knowledge-two>* {
  width: 100%;
  height: 100%;
}

/* 响应式适配 */
@media (max-width: 1024px) {
  .view-panel {
    height: 58vh;
  }

  .knowledge-two {
    grid-template-columns: 1fr;
  }
}
</style>
