a = {
  "body": [
    {
      "node_end": 10.0,
      "col_offset": 0.0,
      "extraAttribute": true,
      "value": {
        "node_end": 10.0,
        "col_offset": 5.0,
        "ast_type": "DictComp",
        "extraAttribute": true,
        "value": {
          "node_end": 12.0,
          "right": {
            "node_end": 12.0,
            "col_offset": 9.0,
            "extraAttribute": true,
            "n": 1.0,
            "node_start": 11.0,
            "ast_type": "Num",
            "lineno": 2.0
          },
          "col_offset": 7.0,
          "extraAttribute": true,
          "op_node": {
            "node_end": 11.0,
            "col_offset": 8.0,
            "extraAttribute": true,
            "node_start": 10.0,
            "ast_type": "Name",
            "lineno": 2.0,
            "id": "+"
          },
          "node_start": 9.0,
          "ast_type": "BinOp",
          "lineno": 2.0,
          "_fields": [
            "left",
            "op",
            "right",
            "op_node"
          ],
          "left": {
            "node_end": 10.0,
            "col_offset": 7.0,
            "extraAttribute": true,
            "ctx": {
              "ast_type": "Load"
            },
            "node_start": 9.0,
            "ast_type": "Name",
            "lineno": 2.0,
            "id": "i"
          },
          "op": {
            "extraAttribute": true,
            "ast_type": "Add"
          }
        },
        "node_start": 7.0,
        "generators": [
          {
            "extraAttribute": true,
            "ast_type": "comprehension",
            "target": {
              "node_end": 18.0,
              "col_offset": 15.0,
              "extraAttribute": true,
              "ctx": {
                "ast_type": "Store"
              },
              "node_start": 17.0,
              "ast_type": "Name",
              "lineno": 2.0,
              "id": "i"
            },
            "iter": {
              "node_end": 26.0,
              "col_offset": 20.0,
              "extraAttribute": true,
              "ctx": {
                "ast_type": "Load"
              },
              "node_start": 22.0,
              "ast_type": "Name",
              "lineno": 2.0,
              "id": "nums"
            },
            "ifs": []
          }
        ],
        "lineno": 2.0,
        "key": {
          "node_end": 8.0,
          "col_offset": 5.0,
          "extraAttribute": true,
          "ctx": {
            "ast_type": "Load"
          },
          "node_start": 7.0,
          "ast_type": "Name",
          "lineno": 2.0,
          "id": "i"
        }
      },
      "node_start": 2.0,
      "ast_type": "Assign",
      "lineno": 2.0,
      "targets": [
        {
          "node_end": 3.0,
          "col_offset": 0.0,
          "extraAttribute": true,
          "ctx": {
            "ast_type": "Store"
          },
          "node_start": 2.0,
          "ast_type": "Name",
          "lineno": 2.0,
          "id": "x"
        }
      ]
    }
  ],
  "node_end": 10.0,
  "extraAttribute": true,
  "filename": "/Users/yinwang/Dropbox/prog/pysonar2/src/org/yinwang/pysonar/tests/deserialize.py",
  "node_start": 2.0,
  "ast_type": "Module"
}
