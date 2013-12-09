require 'ripper'
require 'pp'
require 'json'

# --------------------- utils ---------------------
def banner(s)
  puts "\033[93m#{s}:\033[0m"
end


def dump(input, output)
  src = File.open(input, "rb").read
  tree = Ripper::SexpBuilder.new(src).parse

  banner "sexp"
  pp tree
  simplified = convert(tree)

  banner "simplified"
  pp simplified

  json_string = JSON.pretty_generate(simplified)
  out = File.open(output, "wb")
  out.write(json_string)
end


# convert ruby's "sexp" into json
# also remove redundant information
# exp -> hash
def convert(exp)
  h = {}
  if not exp.is_a?(Array) and not exp==nil
    puts "unexpected input: #{exp.inspect}"
  end
  
  if exp == nil
    {}
  elsif exp[0] == :program
    h[:ast_type] = :program
    h[:body] = convert(exp[1])
    h
  elsif exp[0] == :module
    h[:ast_type] = :module
    h[:name] = convert(exp[1])
    h[:body] = convert(exp[2])
    h
  elsif exp[0] == :def
    h[:ast_type] = :def
    h[:name] = convert(exp[1])
    h[:params] = convert(exp[2])
    h[:body] = convert(exp[3])
    h
  elsif exp[0] == :class
    h[:ast_type] = :class
    h[:name] = convert(exp[1])
    h[:body] = convert(exp[3])
    h
  elsif [:stmts_add, :const_path_ref].include?(exp[0])
    s1 = convert(exp[1])
    s2 = convert(exp[2])
    if not s1.is_a?(Array)
      s1 = [s1]
    end
    
    if not s2.is_a?(Array)
      s2 = [s2]
    end
    
    ret = []
    ret.concat(s1).concat(s2)
    ret
  elsif exp[0] == :assign
    h[:ast_type] = :assign
    h[:target] = convert(exp[1])
    h[:value] = convert(exp[2])
    h
  elsif [:var_field, :var_ref, :const_ref].include? exp[0]
    h[:ast_type] = :name
    h.merge!(convert(exp[1]))
    h
  elsif exp[0] == :params
    h[:ast_type] = :params
    h[:value] = convert_array(exp[1..-1])
    h
  elsif exp[0] == :@ivar
    h[:ast_type] = :ivar
    h[:name] = convert(exp[1])
    h
  elsif exp[0] == :@ident
    h[:id] = exp[1]
    h[:location] = exp[2]
    h
  elsif exp[0] == :if
    h[:ast_type] = :if
    h[:test] = convert(exp[1])
    h[:body] = convert(exp[2])
    if exp[3]
      h[:orelse] = convert(exp[3])
    end
    h
  elsif exp[0] == :binary
    h[:ast_type] = :binary
    h[:left] = convert(exp[1])
    h[:op] = convert(exp[2])
    h[:right] = convert(exp[3])
    h
  elsif exp[0] == :@int
    h[:ast_type] = :int
    h[:n] = exp[1]
    h[:location] = exp[2]
    h
  elsif exp[0] == :@const
    h[:ast_type] = :const
    h[:value] = exp[1]
    h[:location] = exp[2]
    h
  elsif exp[0] == :hash
    h[:ast_type] = :hash
    h[:value] = convert(exp[1])
    h
  elsif [:stmts_new, :void_stmt].include?(exp[0])
    []
  elsif [:vcall, :paren, :else, :bodystmt, :rest_param, :blockarg].include?(exp[0])
    convert(exp[1])
  elsif exp.is_a?(Array)
    convert_array(exp)
  else
    banner("unknown #{exp}")
    h[:unknown] = exp
  end
end


def convert_array(arr)
  arr.map {|x| convert(x)}
end


dump(ARGV[0], ARGV[1])
