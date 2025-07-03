package com.bro.binterface.door.tcp.gaoyou.message;

import com.bro.common.core.constant.DoorConstants;
import lombok.Data;
/**
 * 卡记录数据实体
 * - 注：如修改状态数据，需对应修改getDoorStatusToUpdate方法中的状态
 */
@Data
public class GaoYouReportMessage {
    private String doorNum;
    private String time;
    private String status;
    private String carNo;
    private int type;
    private String doorStatus;

    /**
     * 门禁状态修改
     * 注：如修改状态数据，需对应修改getDoorStatusToUpdate方法中的状态
     * @param doorNum
     * @param time
     * @param status
     * @param carNo
     * @param type
     */
    public GaoYouReportMessage(String doorNum, String time, int status, String carNo, int type) {
        this.doorNum = doorNum;
        this.time = time;
        this.carNo = carNo;
        this.type = type;
        this.doorStatus = "";
        if (type == 1) {
            // 读卡记录
            switch (status) {
                case 1:
                    this.status = "合法开门";
                    this.doorStatus = DoorConstants.COMMAND_OPEN_DOOR;
                    break;
                case 2:
                    this.status = "密码开门";
                    this.doorStatus = DoorConstants.COMMAND_OPEN_DOOR;
                    break;
                case 3:
                    this.status = "卡加密码";
                    break;
                case 4:
                    this.status = "手动输入卡加密码";
                    break;
                case 5:
                    this.status = "首卡开门";
                    this.doorStatus = DoorConstants.COMMAND_OPEN_DOOR;
                    break;
                case 6:
                    this.status = "门常开";
                    this.doorStatus = DoorConstants.COMMAND_OPEN_DOOR;
                    break;
                case 7:
                    this.status = "多卡开门";
                    break;
                case 8:
                    this.status = "重复读卡";
                    break;
                case 9:
                    this.status = "有效期过期";
                    break;
                case 10:
                    this.status = "开门时段过期";
                    break;
                case 11:
                    this.status = "节假日无效";
                    break;
                case 12:
                    this.status = "未注册卡";
                    break;
                case 13:
                    this.status = "巡更卡";
                    break;
                case 14:
                    this.status = "探测锁定";
                    break;
                case 15:
                    this.status = "无有效次数";
                    break;
                case 16:
                    this.status = "防潜回";
                    break;
                case 17:
                    this.status = "密码错误";
                    break;
                case 18:
                    this.status = "密码加卡模式密码错误";
                    break;
                case 19:
                    this.status = "锁定时(读卡)或(读卡加密码)开门";
                    break;
                case 20:
                    this.status = "锁定时(密码开门)";
                    break;
                case 21:
                    this.status = "首卡未开门";
                    break;
                case 22:
                    this.status = "挂失卡";
                    break;
                case 23:
                    this.status = "黑名单卡";
                    break;
                case 24:
                    this.status = "门内上限已满，禁止入门";
                    break;
                case 25:
                    this.status = "开启防盗布防状态(设置卡)";
                    break;
                case 26:
                    this.status = "撤销防盗布防状态(设置卡)";
                    break;
                case 27:
                    this.status = "开启防盗布防状态";
                    break;
                case 28:
                    this.status = "撤销防盗布防状态";
                    break;
                case 29:
                    this.status = "互锁时(读卡)或(读卡加密码)开门";
                    break;
                case 30:
                    this.status = "互锁时(密码开门)";
                    break;
                case 31:
                    this.status = "全卡开门";
                    break;
                case 32:
                    this.status = "多卡开门--等待下张卡";
                    break;
                case 33:
                    this.status = "多卡开门--组合错误";
                    break;
                case 34:
                    this.status = "非首卡时段刷卡无效";
                    break;
                case 35:
                    this.status = "非首卡时段密码无效";
                    break;
                case 36:
                    this.status = "禁止刷卡开门";
                    break;
                case 37:
                    this.status = "禁止密码开门";
                    break;
                case 38:
                    this.status = "门内已刷卡，等待门外刷卡";
                    break;
                case 39:
                    this.status = "门外已刷卡，等待门内刷卡";
                    break;
                case 40:
                    this.status = "请刷管理卡";
                    break;
                case 41:
                    this.status = "请刷普通卡";
                    break;
                case 42:
                    this.status = "首卡未读卡时禁止密码开门";
                    break;
                case 43:
                    this.status = "控制器已过期_刷卡";
                    break;
                case 44:
                    this.status = "控制器已过期_密码";
                    break;
                case 45:
                    this.status = "合法卡开门—有效期即将过期";
                    break;
                case 46:
                    this.status = "拒绝开门--区域反潜回失去主机连接";
                    break;
                case 47:
                    this.status = "拒绝开门--区域互锁，失去主机连接";
                    break;
                case 48:
                    this.status = "区域防潜回--拒绝开门";
                    break;
                case 49:
                    this.status = "区域互锁--有门未关好，拒绝开门";
                    break;
                case 50:
                    this.status = "开门密码有效次数过期";
                    break;
                case 51:
                    this.status = "开门密码有效期过期";
                    break;
                case 52:
                    this.status = "二维码已过期";
                    break;
                default:  this.status = String.valueOf(status);
            }
        } else if(type == 2){
            // 出门开关记录
            switch (status) {
                case 1:
                    this.status = "合法开门";
                    this.doorStatus = DoorConstants.COMMAND_OPEN_DOOR;
                    break;
                case 2:
                    this.status = "开门时段过期";
                    break;
                case 3:
                    this.status = "锁定时按钮";
                    break;
                case 4:
                    this.status = "控制器已过期";
                    break;
                case 5:
                    this.status = "互锁时按钮(不开门)";
                    break;
                default:
                    this.status = String.valueOf(status);
            }
        } else if(type == 3){
            // 门磁记录
            switch (status) {
                case 1:
                    this.status = "开门";
                    this.doorStatus = DoorConstants.COMMAND_OPEN_DOOR;
                    break;
                case 2:
                    this.status = "关门";
                    this.doorStatus = DoorConstants.COMMAND_CLOSE_DOOR;
                    break;
                case 3:
                    this.status = "进入门磁报警状态";
                    break;
                case 4:
                    this.status = "退出门磁报警状态";
                    break;
                case 5:
                    this.status = "门未关好";
                    break;
                default:
                    this.status = String.valueOf(status);
            }
        } else if(type ==4){
            // 软件操作记录
            switch (status) {
                case 1:
                    this.status = "软件开门";
                    this.doorStatus = DoorConstants.COMMAND_OPEN_DOOR;
                    break;
                case 2:
                    this.status = "软件关门";
                    this.doorStatus = DoorConstants.COMMAND_CLOSE_DOOR;
                    break;
                case 3:
                    this.status = "软件常开";
                    break;
                case 4:
                    this.status = "控制器自动进入常开";
                    break;
                case 5:
                    this.status = "控制器自动关闭门";
                    break;
                case 6:
                    this.status = "长按出门按钮常开";
                    break;
                case 7:
                    this.status = "长按出门按钮常闭";
                    break;
                case 8:
                    this.status = "软件锁定";
                    break;
                case 9:
                    this.status = "软件解除锁定";
                    break;
                case 10:
                    this.status = "控制器定时锁定--到时间自动锁定";
                    break;
                case 11:
                    this.status = "控制器定时锁定--到时间自动解除锁定";
                    break;
                case 12:
                    this.status = "报警--锁定";
                    break;
                case 13:
                    this.status = "报警--解除锁定";
                    break;
                case 14:
                    this.status = "互锁时远程开门";
                    break;
                default:
                    this.status = String.valueOf(status);
            }
        } else if (type ==5) {
            // 报警记录
            switch (status) {
                case 1:
                    this.status = "门磁报警";
                    break;
                case 2:
                    this.status = "匪警报警";
                    break;
                case 3:
                    this.status = "消防报警";
                    break;
                case 4:
                    this.status = "非法卡刷报警";
                    break;
                case 5:
                    this.status = "胁迫报警";
                    break;
                case 6:
                    this.status = "消防报警(命令通知)";
                    break;
                case 7:
                    this.status = "烟雾报警";
                    break;
                case 8:
                    this.status = "防盗报警";
                    break;
                case 9:
                    this.status = "黑名单报警";
                    break;
                case 10:
                    this.status = "开门超时报警";
                    break;
                case 0x11:
                    this.status = "门磁报警撤销";
                    break;
                case 0x12:
                    this.status = "匪警报警撤销";
                    break;
                case 0x13:
                    this.status = "消防报警撤销";
                    break;
                case 0x14:
                    this.status = "非法卡刷报警撤销";
                    break;
                case 0x15:
                    this.status = "胁迫报警撤销";
                    break;
                case 0x17:
                    this.status = "撤销烟雾报警";
                    break;
                case 0x18:
                    this.status = "关闭防盗报警";
                    break;
                case 0x19:
                    this.status = "关闭黑名单报警";
                    break;
                case 0x1A:
                    this.status = "关闭开门超时报警";
                    break;
                case 0x21:
                    this.status = "门磁报警撤销(命令通知)";
                    break;
                case 0x22:
                    this.status = "匪警报警撤销(命令通知)";
                    break;
                case 0x23:
                    this.status = "消防报警撤销(命令通知)";
                    break;
                case 0x24:
                    this.status = "非法卡刷报警撤销(命令通知)";
                    break;
                case 0x25:
                    this.status = "胁迫报警撤销(命令通知)";
                    break;
                case 0x27:
                    this.status = "撤销烟雾报警(命令通知)";
                    break;
                case 0x28:
                    this.status = "关闭防盗报警(软件关闭)";
                    break;
                case 0x29:
                    this.status = "关闭黑名单报警(软件关闭)";
                    break;
                case 0x2A:
                    this.status = "关闭开门超时报警";
                    break;
                case 0xB:
                    this.status = "控制板防拆报警";
                    break;
                case 0x1B:
                    this.status = "关闭控制板防拆报警";
                    break;
                case 0xC:
                    this.status = "读卡器防拆报警";
                    break;
                case 0x1C:
                    this.status = "关闭读卡器防拆报警";
                    break;
                default:
                    this.status = String.valueOf(status);
            }
        } else {
            // 系统记录
            switch (status) {
                case 1:
                    this.status = "系统加电";
                    break;
                case 2:
                    this.status = "系统错误复位（看门狗）";
                    break;
                case 3:
                    this.status = "设备格式化记录";
                    break;
                case 4:
                    this.status = "系统高温记录，温度大于>75";
                    break;
                case 5:
                    this.status = "系统UPS供电记录";
                    break;
                case 6:
                    this.status = "温度传感器损坏，温度大于>100";
                    break;
                case 7:
                    this.status = "电压过低，小于<09V";
                    break;
                case 8:
                    this.status = "电压过高，大于>14V";
                    break;
                case 9:
                    this.status = "读卡器接反";
                    break;
                case 10:
                    this.status = "读卡器线路未接好";
                    break;
                case 11:
                    this.status = "无法识别的读卡器";
                    break;
                case 12:
                    this.status = "电压恢复正常，小于14V，大于9V";
                    break;
                case 13:
                    this.status = "网线已断开";
                    break;
                case 14:
                    this.status = "网线已插入";
                    break;
                default:
                    this.status = String.valueOf(status);
            }
        }
    }
}
