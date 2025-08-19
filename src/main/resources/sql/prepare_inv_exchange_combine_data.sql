begin

Insert into JIT_INV_EXCHANGE_HEADER
( 
  external_id   ,
  external_no   ,
  wh_name       ,
  storer        ,
  exchange_type ,
  apply_date    ,
  ref_no        ,
  remark        
)
select distinct 
       we.WIP_ENTITY_ID,
       we.WIP_ENTITY_NAME,
       'GT'  ,
       DECODE(mmt.organization_id,169,'ZCSH','ZTSH') as Storer,
       'Combine',
       CURRENT_TIMESTAMP as ApplyDate,
       we.WIP_ENTITY_NAME,
       null as remark
  FROM MTL_MATERIAL_TRANSACTIONS@PROD2 mmt,
       apps.mtl_item_locations@PROD2 mil,
       WIP_ENTITIES@PROD2 WE 
 WHERE mmt.TRANSACTION_TYPE_ID in (44)
 and mmt.TRANSACTION_SOURCE_ID =we.WIP_ENTITY_ID
 and mmt.TRANSACTION_DATE  >=to_date('2025/07/01','yyyy/mm/dd')
 AND TRUNC(mmt.transaction_date) = TRUNC(sysdate)
 and mmt.ORGANIZATION_ID IN (169,209)
 and mmt.TRANSACTION_QUANTITY >0 
 AND mmt.organization_id = mil.organization_id (+) 
 AND mmt.subinventory_code = mil.subinventory_code(+)
 AND mmt.LOCATOR_ID = mil.inventory_location_id (+) 
 AND (mil.segment1 like '%基通%' OR mil.segment2 like '%基通%')
 AND mil.attribute3='JIT'
 and exists ( select 1  
  FROM MTL_MATERIAL_TRANSACTIONS@PROD2 mmt1,
      apps.mtl_item_locations@PROD2 mil1,
       WIP_ENTITIES@PROD2 WE1 
 WHERE mmt1.TRANSACTION_TYPE_ID in (35)
 and mmt1.TRANSACTION_SOURCE_ID =we1.WIP_ENTITY_ID
 and mmt1.TRANSACTION_DATE  >=to_date('2025/07/01','yyyy/mm/dd')
 AND TRUNC(mmt1.transaction_date) = TRUNC(sysdate)
 and mmt1.ORGANIZATION_ID IN (169,209)
 and mmt1.TRANSACTION_QUANTITY <0 
 and we.WIP_ENTITY_NAME = we1.WIP_ENTITY_NAME
 and mmt.ORGANIZATION_ID = mmt1.ORGANIZATION_ID
  AND mmt1.organization_id = mil1.organization_id (+) 
 AND mmt1.subinventory_code = mil1.subinventory_code(+)
 AND mmt1.LOCATOR_ID = mil1.inventory_location_id (+) 
 AND (mil1.segment1 like '%基通%' OR mil1.segment2 like '%基通%')
 and mil1.attribute3='JIT'
 )
 and NOT EXISTS(
              SELECT 1
              FROM JIT_INV_EXCHANGE_HEADER JIEH
              WHERE 
                 JIEH.external_ID = TO_CHAR(we.WIP_ENTITY_ID) ) ; 
 

Insert into JIT_INV_EXCHANGE_FINAL
( 
  header_id ,
  product   ,
  qty       ,
  mf_sku    ,
  zone_name  
)
select  JIEH.HEADER_ID,
        MSI.SEGMENT1,
        MMT.TRANSACTION_QUANTITY,
        NVL(ZEN_GET_WMS_ITEM_F(MSI.SEGMENT1),MSI.SEGMENT1),
        DECODE (mil.segment1,
                  '', '',
                  (mil.segment1 || '.' || mil.segment2)) as subinventory_code
  FROM MTL_MATERIAL_TRANSACTIONS@PROD2 mmt,
      apps.mtl_item_locations@PROD2 mil,
       WIP_ENTITIES@PROD2 WE,
       mtl_system_items_b@PROD2 msi,
       JIT_INV_EXCHANGE_HEADER JIEH 
 WHERE mmt.TRANSACTION_TYPE_ID in (44)
 and mmt.TRANSACTION_SOURCE_ID =we.WIP_ENTITY_ID
 and mmt.TRANSACTION_DATE  >=to_date('2025/07/01','yyyy/mm/dd')
 AND TRUNC(mmt.transaction_date) = TRUNC(sysdate)
 and mmt.ORGANIZATION_ID IN (169,209)
 and mmt.TRANSACTION_QUANTITY >0 
 and mmt.inventory_item_id = msi.INVENTORY_ITEM_ID 
 and mmt.organization_id = msi.ORGANIZATION_ID 
 AND mmt.organization_id = mil.organization_id (+) 
 AND mmt.subinventory_code = mil.subinventory_code(+)
 AND mmt.LOCATOR_ID = mil.inventory_location_id (+) 
 AND (mil.segment1 like '%基通%' OR mil.segment2 like '%基通%')
 AND mil.attribute3='JIT'
 AND JIEH.EXTERNAL_ID = to_char(we.WIP_ENTITY_ID)
 AND JIEH.status ='PENDING'
 and exists ( select 1  
  FROM MTL_MATERIAL_TRANSACTIONS@PROD2 mmt1,
       apps.mtl_item_locations@PROD2 mil1,
       WIP_ENTITIES@PROD2 WE1 
 WHERE mmt1.TRANSACTION_TYPE_ID in (35)
 and mmt1.TRANSACTION_SOURCE_ID =we1.WIP_ENTITY_ID
 and mmt1.TRANSACTION_DATE  >=to_date('2025/07/01','yyyy/mm/dd')
 AND TRUNC(mmt1.transaction_date) = TRUNC(sysdate)
 and mmt1.ORGANIZATION_ID IN (169,209)
 and mmt1.TRANSACTION_QUANTITY <0 
 and we.WIP_ENTITY_NAME = we1.WIP_ENTITY_NAME
 and mmt.ORGANIZATION_ID = mmt1.ORGANIZATION_ID
 AND mmt1.organization_id = mil1.organization_id (+) 
 AND mmt1.subinventory_code = mil1.subinventory_code(+)
 AND mmt1.LOCATOR_ID = mil1.inventory_location_id (+) 
 AND (mil1.segment1 like '%基通%' OR mil1.segment2 like '%基通%')
 AND mil1.attribute3='JIT'
 )
 AND NOT EXISTS(
             SELECT 1
              FROM  JIT_INV_EXCHANGE_FINAL JIEF
              WHERE 
                 JIEH.header_id = JIEF.header_id ) ;  


 Insert into JIT_INV_EXCHANGE_MATERIAL
( 
  final_id    ,
  material    ,
  qty         ,
  mf_sku      ,
  zone_name   
)

SELECT  JIEF.FINAL_ID,
        TRX_OUT.SEGMENT1,
        TRX_OUT.TRANSACTION_QUANTITY,
        TRX_OUT.mf_sku,
        TRX_OUT.subinventory_code
    FROM 
(
select  jieh.header_id,
        MSI.ORGANIZATION_ID,
        MSI.SEGMENT1 AS ITEM_NO ,
        MMT.TRANSACTION_QUANTITY,
        MMT.TRANSACTION_SOURCE_ID
  FROM MTL_MATERIAL_TRANSACTIONS@PROD2 mmt,
       apps.mtl_item_locations@PROD2 mil,
      WIP_ENTITIES@PROD2 WE,
      mtl_system_items_b@PROD2 msi,
      JIT_INV_EXCHANGE_HEADER JIEH 
 WHERE mmt.TRANSACTION_TYPE_ID in (44)
 and mmt.TRANSACTION_SOURCE_ID =we.WIP_ENTITY_ID
 and mmt.TRANSACTION_DATE  >=to_date('2025/07/01','yyyy/mm/dd')
 AND TRUNC(mmt.transaction_date) = TRUNC(sysdate)
 and mmt.ORGANIZATION_ID IN (169,209)
 and mmt.TRANSACTION_QUANTITY >0 
 and mmt.inventory_item_id = msi.INVENTORY_ITEM_ID 
 AND mmt.organization_id = mil.organization_id (+) 
 AND mmt.subinventory_code = mil.subinventory_code(+)
 AND mmt.LOCATOR_ID = mil.inventory_location_id (+) 
AND (mil.segment1 like '%基通%' OR mil.segment2 like '%基通%')
AND mil.attribute3='JIT'
 and mmt.organization_id = msi.ORGANIZATION_ID
 AND JIEH.EXTERNAL_ID = to_char(we.WIP_ENTITY_ID)
AND JIEH.status ='PENDING'  ) TRX_IN ,
 ( select jieh.header_id,MSI1.ORGANIZATION_ID,
        MSI1.SEGMENT1,
        ABS(MMT1.TRANSACTION_QUANTITY) AS TRANSACTION_QUANTITY,
        MMT1.TRANSACTION_SOURCE_ID,
        DECODE (mil1.segment1,
                  '', '',
                  (mil1.segment1 || '.' || mil1.segment2)) as subinventory_code,
        NVL(ZEN_GET_WMS_ITEM_F(MSI1.SEGMENT1) ,MSI1.SEGMENT1) AS mf_sku         
  FROM MTL_MATERIAL_TRANSACTIONS@PROD2 mmt1,
      apps.mtl_item_locations@PROD2 mil1,
       WIP_ENTITIES@PROD2 WE1 ,
       mtl_system_items_b@PROD2 msi1,
        JIT_INV_EXCHANGE_HEADER JIEH 
 WHERE mmt1.TRANSACTION_SOURCE_ID =we1.WIP_ENTITY_ID
  and mmt1.TRANSACTION_TYPE_ID in (35)
 and mmt1.TRANSACTION_DATE  >=to_date('2025/07/01','yyyy/mm/dd')
 AND TRUNC(mmt1.transaction_date) = TRUNC(sysdate)
 and mmt1.ORGANIZATION_ID IN (169,209)
 and mmt1.TRANSACTION_QUANTITY <0 
 AND mmt1.inventory_item_id = msi1.INVENTORY_ITEM_ID 
 and mmt1.organization_id = msi1.ORGANIZATION_ID
 AND mmt1.organization_id = mil1.organization_id (+) 
 AND mmt1.subinventory_code = mil1.subinventory_code(+)
 AND mmt1.LOCATOR_ID = mil1.inventory_location_id (+) 
 AND (mil1.segment1 like '%基通%' OR mil1.segment2 like '%基通%')
 AND mil1.attribute3='JIT'
 AND JIEH.EXTERNAL_ID = to_char(we1.WIP_ENTITY_ID)
 AND JIEH.status ='PENDING'  
  ) TRX_OUT,
  JIT_INV_EXCHANGE_FINAL JIEF 
 WHERE TRX_IN.TRANSACTION_SOURCE_ID =  TRX_OUT .TRANSACTION_SOURCE_ID
 AND TRX_IN.ORGANIZATION_ID = TRX_OUT.ORGANIZATION_ID
 AND TRX_IN.HEADER_ID = TRX_OUT.HEADER_ID 
AND TRX_IN.HEADER_ID = JIEF.HEADER_ID
AND TRX_IN.ITEM_NO = JIEF.PRODUCT
AND NOT EXISTS(
             SELECT 1
              FROM  JIT_INV_EXCHANGE_HEADER JEH ,
                    JIT_INV_EXCHANGE_FINAL JEF,
                    JIT_INV_EXCHANGE_MATERIAL JIM
              WHERE JEH.HEADER_ID = JEF.HEADER_ID 
                 AND TRX_OUT.header_id  = JEH.header_id 
                 AND JIEF.FINAL_ID = JEF.FINAL_ID 
                 AND JEF.FINAL_ID = JIM.FINAL_ID) ;  
end;
