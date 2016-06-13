create database if not exists workflow_test;
use workflow_test;

--
-- 工作流相关
--
drop table COP_WORKFLOW_INSTANCE_ERROR;
drop table COP_WORKFLOW_INSTANCE;
drop table COP_WAIT;
drop table COP_RESPONSE;
drop table COP_QUEUE;
drop table COP_AUDIT_TRAIL_EVENT;
drop table COP_ADAPTERCALL;
drop table COP_LOCK;

--
-- BUSINESSPROCESS
--
create table COP_WORKFLOW_INSTANCE  (
   ID                   VARCHAR(128) not null,
   STATE                TINYINT not null,
   PRIORITY             TINYINT not null,
   LAST_MOD_TS          TIMESTAMP not null,
   PPOOL_ID             VARCHAR(32) not null,
   DATA                 MEDIUMTEXT null,
   OBJECT_STATE         MEDIUMTEXT null,
   CS_WAITMODE          TINYINT,
   MIN_NUMB_OF_RESP     SMALLINT,
   NUMB_OF_WAITS        SMALLINT,
   TIMEOUT              TIMESTAMP,
   CREATION_TS          TIMESTAMP not null,
   CLASSNAME            VARCHAR(512) not null,
   PRIMARY KEY (ID)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
 

create table COP_WORKFLOW_INSTANCE_ERROR (
   WORKFLOW_INSTANCE_ID     VARCHAR(128)    not null,
   EXCEPTION                TEXT            not null,
   ERROR_TS                 TIMESTAMP       not null
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

create index IDX_COP_WFID_WFID on COP_WORKFLOW_INSTANCE_ERROR (
   WORKFLOW_INSTANCE_ID
);

--
-- RESPONSE
--
create table COP_RESPONSE  (
   RESPONSE_ID      VARCHAR(128) not null,
   CORRELATION_ID   VARCHAR(128) not null,
   RESPONSE_TS      TIMESTAMP not null,
   RESPONSE         MEDIUMTEXT,
   RESPONSE_TIMEOUT  TIMESTAMP,
   RESPONSE_META_DATA VARCHAR(4000),
   PRIMARY KEY (RESPONSE_ID)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

 
create index IDX_COP_RESP_CID on COP_RESPONSE (
   CORRELATION_ID
);

--
-- WAIT
--
create table COP_WAIT (
    CORRELATION_ID          VARCHAR(128) not null,
    WORKFLOW_INSTANCE_ID    VARCHAR(128) not null,
    MIN_NUMB_OF_RESP        SMALLINT not null,
    TIMEOUT_TS              TIMESTAMP,
    STATE                   TINYINT not null,
    PRIORITY                TINYINT not null,
    PPOOL_ID                VARCHAR(32) not null,
    PRIMARY KEY (CORRELATION_ID)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;


create index IDX_COP_WAIT_WFI_ID on COP_WAIT (
   WORKFLOW_INSTANCE_ID
);

--
-- QUEUE
--
create table COP_QUEUE (
   PPOOL_ID             VARCHAR(32)                 not null,
   PRIORITY             TINYINT                         not null,
   LAST_MOD_TS          TIMESTAMP                       not null,
   WORKFLOW_INSTANCE_ID VARCHAR(128)                    not null,
   PRIMARY KEY (WORKFLOW_INSTANCE_ID)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;


create table COP_AUDIT_TRAIL_EVENT (
    SEQ_ID                  BIGINT NOT NULL AUTO_INCREMENT,
    OCCURRENCE              TIMESTAMP NOT NULL,
    CONVERSATION_ID         VARCHAR(64) NOT NULL,
    LOGLEVEL                TINYINT NOT NULL,
    CONTEXT                 VARCHAR(128) NOT NULL,
    INSTANCE_ID             VARCHAR(128) NULL,
    CORRELATION_ID          VARCHAR(128) NULL,
    TRANSACTION_ID          VARCHAR(128) NULL,
    LONG_MESSAGE            LONGTEXT NULL,
    MESSAGE_TYPE            VARCHAR(256) NULL,
    PRIMARY KEY (SEQ_ID)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;


CREATE TABLE COP_ADAPTERCALL (WORKFLOWID  VARCHAR(128) NOT NULL,
                          ENTITYID    VARCHAR(128) NOT NULL,
                          ADAPTERID   VARCHAR(128) NOT NULL,
                          PRIORITY    BIGINT NOT NULL,
                          DEFUNCT     CHAR(1) DEFAULT '0' NOT NULL ,
                          DEQUEUE_TS  TIMESTAMP , 
                          METHODDECLARINGCLASS VARCHAR(1024)  NOT NULL,
                          METHODNAME VARCHAR(1024)  NOT NULL,
                          METHODSIGNATURE VARCHAR(2048)  NOT NULL,
                          ARGS LONGTEXT,
                          PRIMARY KEY (ADAPTERID, WORKFLOWID, ENTITYID))
 ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE INDEX COP_IDX_ADAPTERCALL ON COP_ADAPTERCALL(ADAPTERID, PRIORITY);

--
-- COP_LOCK
--
create table COP_LOCK (
    LOCK_ID                 VARCHAR(128) NOT NULL, 
    CORRELATION_ID          VARCHAR(128) NOT NULL, 
    WORKFLOW_INSTANCE_ID    VARCHAR(128) NOT NULL, 
    INSERT_TS               TIMESTAMP NOT NULL, 
    REPLY_SENT              CHAR(1) NOT NULL,
    PRIMARY KEY (LOCK_ID,WORKFLOW_INSTANCE_ID)
)
 ENGINE=InnoDB DEFAULT CHARSET=utf8;
 
 
--
-- 定时器
--
DROP TABLE `schedulerconfig`;

CREATE TABLE `schedulerconfig` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'AI',
  `schedulerName` varchar(64) NOT NULL COMMENT 'scheduler名字, 应该在SchedulerDef里面定义',
  `triggerType` varchar(64) NOT NULL COMMENT '触发类型, 参照TriggerType枚举类型目前支持INTERVAL_REPEAT,TIME_REPEAT,RULE',
  `enabled` tinyint(1) NOT NULL DEFAULT '1' COMMENT '是否启用1为启用, 0为不启用, 不影响当前正在运行的scheduler',
  `repeatInterval` int(10) unsigned DEFAULT NULL COMMENT '下次执行间隔时间INTERVAL_REPEAT类型必填',
  `timeUnit` varchar(16) DEFAULT NULL COMMENT 'repeatInterval 的时间单位, 目前支持的: MILLISECONDS,SECONDS,MINUTES,HOURS,DAYS, INTERVAL_REPEAT类型必填',
  `cronExp` varchar(64) DEFAULT NULL COMMENT 'TIME_REPEAT类型必填, 使用的触发cron expression其它类型无视',
  `firstRunTime` datetime DEFAULT NULL COMMENT '首次触发时间, 如果为空则等待配置的间隔时间',
  `totalRunCount` int(10) unsigned DEFAULT NULL COMMENT '总执行次数, 如果为空则永远执行下去',
  `noCatchUp` tinyint(1) unsigned DEFAULT '0' COMMENT '如果设为true则不会在当前时间比下次执行时间晚的情况下立即触发任务. 例子: 上次触发时间是8:30分, 设定是每30分钟触发一次, 然后因为系统维护, 启动的时候当然已经9:10了,如果noCatchUp=true则下次执行时间是9:30, 如果false则马上会执行',
  `defaultPoolId` varchar(255) DEFAULT NULL COMMENT '默认工作流池ID, 如果为空则进入默认池',
  `defaultPriority` int(10) unsigned DEFAULT NULL COMMENT '默认执行优先级, 如果为空则设为默认优先级(5), 数字越小优先级越高, 1为最高',
  `createdBy` bigint(20) NOT NULL,
  `createdDate` datetime NOT NULL,
  `modifiedBy` bigint(20) NOT NULL,
  `modifiedDate` datetime NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UNI_schedulerconfig_schedulerName` (`schedulerName`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8 COMMENT='所有定时任务都需要在这里配置默认属性';
