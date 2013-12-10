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


def dump_string(src)
  tree = Ripper::SexpBuilder.new(src).parse

  banner "sexp"
  pp tree
  simplified = convert(tree)

  banner "simplified"
  pp simplified
end


# convert ruby's "sexp" into json
# also remove redundant information
# exp -> hash
def convert(exp)
  h = {}

  if exp == nil
    {}
  elsif exp[0] == :program
    {
      :astype => :program,
      :body => convert(exp[1])
    }
  elsif exp[0] == :module
    {
      :astype => :module,
      :name => convert(exp[1]),
      :body => convert(exp[2])
    }
  elsif exp[0] == :@ident
    {
      :id => exp[1],
      :location => exp[2]
    }
  elsif exp[0] == :@ivar
    {
      :astype => :ivar,
      :name => convert(exp[1])
    }
  elsif exp[0] == :@const
    #:@const is just a name
    {
      :astype => :name,
      :value => exp[1],
      :location => exp[2]
    }
  elsif exp[0] == :def
    {
      :astype => :def,
      :name => convert(exp[1]),
      :params => convert(exp[2]),
      :body => convert(exp[3])
    }
  elsif exp[0] == :params
    {
      :astype => :params,
      :value => convert_array(exp[1..-1])
    }
  elsif exp[0] == :class
    {
      :astype => :class,
      :name => convert(exp[1]),
      :body => convert(exp[3])
    }
  elsif exp[0] == :method_add_block
    call = convert(exp[1])
    call[:block] = convert(exp[2])
    call
  elsif exp[0] == :method_add_arg
    call = convert(exp[1])
    call[:args].push(convert(exp[2]))
    call
  elsif exp[0] == :fcall or exp[0] == :command
    {
      :astype => :call,
      :func => convert(exp[1]),
      :args => []
    }
  elsif exp[0] == :args_new
    {
      :astype => :args,
      :args => []
    }
  elsif exp[0] == :args_add
    args = convert(exp[1])
    args[:args].push(convert(exp[2]))
    args
  elsif exp[0] == :args_add_block
    args = convert(exp[1])
    maybe_block = exp[2]
    if maybe_block
      args[:block] = maybe_block
    end
    args
  elsif exp[0] == :assign
    {
      :astype => :assign,
      :target => convert(exp[1]),
      :value => convert(exp[2])
    }
  elsif exp[0] == :if
    ret = {
      :astype => :if,
      :test => convert(exp[1]),
      :body => convert(exp[2])
    }
    if exp[3]
      ret[:orelse] = convert(exp[3])
    end
    ret
  elsif exp[0] == :stmts_new
    {
      :astype => :block,
      :stmts => []
    }
  elsif exp[0] == :stmts_add
    block = convert(exp[1])
    stmt = convert(exp[2])
    block[:stmts].push(stmt)
    block
  elsif exp[0] == :binary
    {
      :astype => :binary,
      :left => convert(exp[1]),
      :op => convert(exp[2]),
      :right => convert(exp[3])
    }
  elsif exp[0] == :@int
    {
      :astype => :int,
      :n => exp[1],
      :location => exp[2]
    }
  elsif exp[0] == :hash
    {
      :astype => :hash,
      :value => convert(exp[1])
    }
  elsif exp[0] == :const_path_ref
    {
      :astype => :attribute,
      :value => convert(exp[1]),
      :attr => convert(exp[2])
    }
  elsif exp[0] == :void_stmt
    {
      :astype => :void
    }
  # superflous wrappers that contains one object, just remove it
  elsif is_wrapper?(exp[0])
    convert(exp[1])
  else
    banner("unknown")
    puts "#{exp}"
  end
end


def convert_array(arr)
  arr.map {|x| convert(x)}
end


def is_wrapper?(x)
  wrappers = [:var_field,
              :var_ref,
              :const_ref,
              :vcall,
              :paren,
              :arg_paren,
              :else,
              :bodystmt,
              :rest_param,
              :blockarg
             ]
  return wrappers.include?(x)
end

if ARGV[0] == "-s"
  dump_string(ARGV[1])
elsif ARGV[0] == "-c"
  pp convert(ARGV[1])
else
  dump(ARGV[0], ARGV[1])
end
