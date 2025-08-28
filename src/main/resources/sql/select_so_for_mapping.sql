SELECT
    ord_type,
    exp_ship_date,
    invoice_no,
    cust_name,
    invoice_address,
    ship_to_no,
    ship_to_phone,
    ship_to_contact,
    delivery_address
FROM
    (SELECT
        ord_type,
        exp_ship_date,
        invoice_no,
        cust_name,
        invoice_address,
        ship_to_no,
        ship_to_phone,
        ship_to_contact,
        delivery_address,
        status,
        ROW_NUMBER() OVER (PARTITION BY invoice_no ORDER BY SEQ_ID DESC) AS rn
    FROM
        ZEN_B2B_JSON_SO
    WHERE
        RECEIVER_CODE = ?)
WHERE
    rn = 1
AND
    status = 'W' 