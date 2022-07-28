# JSON_DB-Project
Part of JetBrains "Java Developer" course

Local client-server JSON database. Stored in "./src/server/data/db.json".
Able to read info from command line, or from the file on the client side.

Commands: "set" new json object, "get" json object, "delete" the object and "exit" the program.

Command arguments: "-t" - type of the request (look up)
                   "-k" - index of the cell. For example -k "person" - gives all the person info; "person, name" - only name
                   "-v" - input value. "Hello World!"
                   "-in" - read json request from the file. Example: {"type":"set","key":["person","rocket","launches"],"value":"88"}
