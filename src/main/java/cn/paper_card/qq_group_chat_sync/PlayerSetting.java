package cn.paper_card.qq_group_chat_sync;

class PlayerSetting {
    private boolean receiveGroupMsg = true;

    boolean isReceiveGroupMsg() {
        return this.receiveGroupMsg;
    }

    void setReceiveGroupMsg(boolean receiveGroupMsg) {
        this.receiveGroupMsg = receiveGroupMsg;
    }
}
