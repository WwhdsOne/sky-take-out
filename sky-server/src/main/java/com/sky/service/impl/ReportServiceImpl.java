package com.sky.service.impl;

import com.sky.dto.GoodsSalesDTO;
import com.sky.entity.Orders;
import com.sky.mapper.OrderDetailMapper;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.UserMapper;
import com.sky.service.ReportService;
import com.sky.service.WorkspaceService;
import com.sky.vo.*;
import io.swagger.models.auth.In;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ReportServiceImpl implements ReportService {

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private OrderDetailMapper orderDetailMapper;

    @Autowired
    private WorkspaceService workspaceService;

    private List<LocalDate> getDateListByBeginAndEnd(LocalDate begin,LocalDate end){
        List<LocalDate> dateList = new ArrayList<>();
        dateList.add(begin);
        while ( !begin.equals(end) ) {
            begin = begin.plusDays(1);
            dateList.add(begin);
        }
        return dateList;
    }

    /**
     * 根据日期状态获取订单数量
     * @param beginTime
     * @param endTime
     * @param status
     * @return
     */
    private Integer getOrderCount(LocalDateTime beginTime,LocalDateTime endTime,Integer status){
        Map map = new HashMap();
        map.put("beginTime",beginTime);
        map.put("endTime",endTime);
        map.put("status",status);
        return orderMapper.countByMap(map);
    }
    /**
     * 统计指定区间内的营业额数据
     *
     * @param begin
     * @param end
     * @return
     */
    @Override
    public TurnoverReportVO getTurnoverStatistics(LocalDate begin, LocalDate end) {
        //此集合用于存放begin到end的日期集合(闭区间)
        List<LocalDate> dateList = getDateListByBeginAndEnd(begin,end);

        List<Double> turnoverList = new ArrayList<>();
        //查询date日期对应的营业额
        for ( LocalDate date : dateList ) {
            //查询date对应日期已完成的订单
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);
            //select sum(amount) from orders where order_time > #{beginTime}  and order_time < #{endTime} and status = 5;
            //通过map传递参数
            Map map = new HashMap();
            map.put("begin", beginTime);
            map.put("end", endTime);
            map.put("status", Orders.COMPLETED);
            Double turnover = orderMapper.sumByMap(map);

            turnover = turnover == null ? 0.0 : turnover;
            turnoverList.add(turnover);
        }
        return TurnoverReportVO
                .builder()
                .dateList(StringUtils.join(dateList, ","))
                .turnoverList(StringUtils.join(turnoverList, ","))
                .build();
    }

    //统计指定日期用户新增数量和总用户数量
    @Override
    public UserReportVO getUserStatistics(LocalDate begin, LocalDate end) {
        //此集合用于存放begin到end的日期集合(闭区间)
        List<LocalDate> dateList = getDateListByBeginAndEnd(begin,end);


        //存放每日新增用户数量
        //select count(id) from user where createTime > #{} and createTime < #{}
        List<Integer> newUserList = new ArrayList<>();


        //存放总用户数量
        //select count(id) from user where createTime <= #{};
        List<Integer> totalUserList = new ArrayList<>();



        for ( LocalDate date : dateList ) {
            Map map = new HashMap();
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);
            map.put("endTime",endTime);
            totalUserList.add(userMapper.countByMap(map));
            map.put("beginTime",beginTime);
            newUserList.add(userMapper.countByMap(map));
        }
        return UserReportVO.builder()
                .dateList(StringUtils.join(dateList,","))
                .newUserList(StringUtils.join(newUserList,","))
                .totalUserList(StringUtils.join(totalUserList,","))
                .build();
    }

    /**
     * 订单数据统计
     * @param begin
     * @param end
     * @return
     */
    @Override
    public OrderReportVO getOrdersStatistics(LocalDate begin, LocalDate end) {
        List<LocalDate> dateList = getDateListByBeginAndEnd(begin,end);
        List<Integer> orderCountList = new ArrayList<>();
        List<Integer> validOrderCountList = new ArrayList<>();

        for ( LocalDate date : dateList ) {
            Map map = new HashMap();
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);
            //查询每天订单总数
            //select count(id) from orders where order_time < #{endTime} and order_time > #{}
            orderCountList.add(getOrderCount(beginTime,endTime,null));
            //查询每天有效订单数
            //select count(id) from orders where order_time < #{endTime} and order_time > #{} and status = #{}
            validOrderCountList.add(getOrderCount(beginTime,endTime,Orders.COMPLETED));
        }
        //计算订单总数
        Integer totalOrderCount = orderCountList.stream().reduce(0, Integer::sum);
        //计算有效订单总数
        Integer validOrderCount = validOrderCountList.stream().reduce(0, Integer::sum);
        //订单完成率
        Double orderCompletionRate = 0.0;
        //当有效订单数为0时特判
        if(totalOrderCount != 0){
            orderCompletionRate = validOrderCount.doubleValue() / totalOrderCount;
        }


        return OrderReportVO.builder()
                .dateList(StringUtils.join(dateList,","))
                .orderCountList(StringUtils.join(orderCountList,","))
                .validOrderCountList(StringUtils.join(validOrderCountList,","))
                .totalOrderCount(totalOrderCount)
                .validOrderCount(validOrderCount)
                .orderCompletionRate(orderCompletionRate)
                .build();
    }

    /**
     * 查询销量排名top10
     * @param begin
     * @param end
     * @return
     */
    @Override
    public SalesTop10ReportVO getSalesTop10(LocalDate begin, LocalDate end) {
        LocalDateTime beginTime = LocalDateTime.of(begin, LocalTime.MIN);
        LocalDateTime endTime = LocalDateTime.of(end, LocalTime.MAX);
        log.info("top10统计:{},{}",beginTime,endTime);
        List<GoodsSalesDTO> salesTop10 = orderMapper.getSalesTop10(beginTime, endTime);

        //通过流获取菜品名和销量集合
        List<String> nameList = salesTop10.stream().map(GoodsSalesDTO::getName).collect(Collectors.toList());
        List<Integer> numberList = salesTop10.stream().map(GoodsSalesDTO::getNumber).collect(Collectors.toList());

        return SalesTop10ReportVO.builder()
                .nameList(StringUtils.join(nameList,","))
                .numberList(StringUtils.join(numberList,","))
                .build();
    }

    /**
     * 导出Excel报表接口
     * @param httpServletResponse
     */
    @Override
    public void exportBusinessData(HttpServletResponse httpServletResponse) {
        //获取近30天营业数据
        LocalDate dateBegin = LocalDate.now().minusDays(30);
        //今天还没结束所以到昨天截止
        LocalDate dateEnd = LocalDate.now().minusDays(1);
        BusinessDataVO businessData = workspaceService.getBusinessData(LocalDateTime.of(dateBegin, LocalTime.MIN), LocalDateTime.of(dateEnd, LocalTime.MAX));
        //通过POI写入excel文件中

        //通过反射获取resources资源路径
        InputStream input = this.getClass().getClassLoader().getResourceAsStream("template/运营数据报表模板.xlsx");
        try {
            XSSFWorkbook excel = new XSSFWorkbook(input);

            XSSFSheet sheet = excel.getSheet("Sheet1");



            //第二行 时间信息s
            sheet.getRow(1).getCell(1).setCellValue("时间" + dateBegin + "至" + dateEnd);
            //第四行 营业额,订单完成率,新增用户数
            sheet.getRow(3).getCell(2).setCellValue(businessData.getTurnover());
            sheet.getRow(3).getCell(4).setCellValue(businessData.getOrderCompletionRate());
            sheet.getRow(3).getCell(6).setCellValue(businessData.getNewUsers());

            //第五行 有效订单,平均客单价
            sheet.getRow(4).getCell(2).setCellValue(businessData.getValidOrderCount());
            sheet.getRow(4).getCell(4).setCellValue(businessData.getUnitPrice());

            //第八行 明细数据

            for ( int i = 0; i < 30; i++ ) {
                //获取当天数据
                LocalDate date = dateBegin.plusDays(1);
                BusinessDataVO dayBusinessData = workspaceService.getBusinessData(LocalDateTime.of(date, LocalTime.MIN), LocalDateTime.of(date, LocalTime.MAX));
                sheet.getRow(7+i).getCell(1).setCellValue(String.valueOf(date));
                sheet.getRow(7+i).getCell(2).setCellValue(dayBusinessData.getTurnover());
                sheet.getRow(7+i).getCell(3).setCellValue(dayBusinessData.getValidOrderCount());
                sheet.getRow(7+i).getCell(4).setCellValue(dayBusinessData.getOrderCompletionRate());
                sheet.getRow(7+i).getCell(5).setCellValue(dayBusinessData.getUnitPrice());
                sheet.getRow(7+i).getCell(6).setCellValue(dayBusinessData.getNewUsers());


            }


            //通过输入流将excel文件下载到客户端
            ServletOutputStream servletOutputStream = httpServletResponse.getOutputStream();
            excel.write(servletOutputStream);
            servletOutputStream.close();
            excel.close();


        } catch (IOException e) {
            e.printStackTrace();
        }
        //填充数据

    }
}
