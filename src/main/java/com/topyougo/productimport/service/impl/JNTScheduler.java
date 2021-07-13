package com.topyougo.productimport.service.impl;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.topyougo.productimport.constant.Courier;
import com.topyougo.productimport.constant.OrderStatus;
import com.topyougo.productimport.constant.TrackingStatus;
import com.topyougo.productimport.dto.DataResponse;
import com.topyougo.productimport.dto.ResponseDetails;
import com.topyougo.productimport.model.Orders;
import com.topyougo.productimport.repository.OrderRepository;
import com.topyougo.productimport.service.JntIntegrationService;

@Component
public class JNTScheduler {

	private Logger logger = LoggerFactory.getLogger(this.getClass());

	@Autowired
	private OrderRepository orderRepository;

	@Autowired
	private JntIntegrationService jntService;

	@Scheduled(fixedRate = 20000)
	public void scheduleFixedDelayTask() {
		List<Orders> orders = orderRepository.findAllByCourierOrderByIDDesc(Courier.JNT.getValue());
		logger.info("Get orders count ", orders.size());

		List<DataResponse> jntListOrders = new ArrayList<DataResponse>();		
		
		for(Orders order : CollectionUtils.emptyIfNull(orders)) {
			logger.info("Get tracking number ", order.getTrackingNumber());
			DataResponse jntResponse = jntService.fetchTrackingInfo(order.getTrackingNumber());
			jntListOrders.add(jntResponse);
		}

		for(DataResponse jntResponse : CollectionUtils.emptyIfNull(jntListOrders)) {
			for (ResponseDetails response : CollectionUtils.emptyIfNull(jntResponse.getDetails())) {
				
				Orders order = orderRepository.findOrdersByTrackingNumber(jntResponse.getBillcode());

				logger.debug("Get billcode and ScanStatus", jntResponse.getBillcode() + " " + response.getScanstatus());

				if (response.getScanstatus().equals("Returned")) {
					order.setTrackingStatus(TrackingStatus.RETURN_TO_SENDER);
					order.setOrderStatus(OrderStatus.RTS);
				} else if (response.getScanstatus().equals("On Return")) {
					order.setTrackingStatus(TrackingStatus.RRTS);
					order.setOrderStatus(OrderStatus.RRTS);
				} else if (response.getScanstatus().equals("Delivered")) {
					order.setTrackingStatus(TrackingStatus.DELIVERED);
					order.setOrderStatus(OrderStatus.DELIVERED);
				} else {
					order.setTrackingStatus(TrackingStatus.SHIPPED);
					order.setOrderStatus(OrderStatus.SHIPPED);
				}
				orderRepository.save(order);
			}
		}
	}
}
