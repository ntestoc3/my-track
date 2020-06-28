# my-track

基金预估追踪，并进行微信消息通知

## Usage

### 1 注册微信消息推送WxPusher
http://wxpusher.zjiecode.com/demo/

设置回调地址 http://[server-ip]:8088/user/add

### 2 添加配置文件config.edn

```clojure
{
 :wxpush-token "" ;; 注册的微信push app-token
 :shangzheng-up 2900 ;; 上证指数增长界限
 :guzhi-up 1.0 ;; 估值增长界限
 :guzhi-down 1.1 ;; 估值下降界限
 :users #{}
 :server-port 8088 ;; http服务监听端口
 :funds-code ["470009" "470006"] ;; 基金代码
 :cron-configs [{:job-key "funds-check.job.1"
                 :trigger-key "funds-check.trigger.1"
                 :schedule "0 50 14 ? * MON-FRI"} ;; 周一到周五，每天14:50执行
                ]
 }
```

### 3 运行主程序
    $ java -jar my-track-0.1.0-standalone.jar 

    访问 http://[server-ip]:8088/qrcode  微信扫码，注册消息推送

## License

Copyright © 2020 ntestoc3

This program and the accompanying materials are made available under the
terms of the Eclipse Public License 2.0 which is available at
http://www.eclipse.org/legal/epl-2.0.

This Source Code may also be made available under the following Secondary
Licenses when the conditions for such availability set forth in the Eclipse
Public License, v. 2.0 are satisfied: GNU General Public License as published by
the Free Software Foundation, either version 2 of the License, or (at your
option) any later version, with the GNU Classpath Exception which is available
at https://www.gnu.org/software/classpath/license.html.
