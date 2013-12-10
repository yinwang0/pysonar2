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
      :type => :program,
      :body => convert(exp[1])
    }
  elsif exp[0] == :module
    {
      :type => :module,
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
      :type => :ivar,
      :name => convert(exp[1])
    }
  elsif exp[0] == :@const
    #:@const is just a name
    {
      :type => :name,
      :value => exp[1],
      :location => exp[2]
    }
  elsif exp[0] == :def
    {
      :type => :def,
      :name => convert(exp[1]),
      :params => convert(exp[2]),
      :body => convert(exp[3])
    }
  elsif exp[0] == :do_block
    {
      :type => :do_block,
      :params => convert(exp[1]),
      :body => convert(exp[2])
    }
  elsif exp[0] == :params
    ret = { :type => :params }
    if exp[1]
      ret[:positional] = convert_array(exp[1])
    end
    if exp[2]
      ret[:keyword] = exp[2].map { |x| make_keyword(x) }
    end
    if exp[3]
      ret[:rest] = convert(exp[3])
    end
    if exp[4]
      ret[:after_rest] = convert_array(exp[4])
    end

    if exp[7]
      ret[:block] = convert(exp[7])
    end

    ret
  elsif exp[0] == :block_var
    params = convert(exp[1])
    if exp[2]
      params[:block_var] = convert_array(exp[2])
    end
    params
  elsif exp[0] == :class
    ret = {
      :type => :class,
      :name => convert(exp[1]),
      :body => convert(exp[3])
    }
    if exp[2]
      ret[:super] = convert(exp[2])
    end
    ret
  elsif exp[0] == :method_add_block
    call = convert(exp[1])
    call[:block_arg] = convert(exp[2])
    call
  elsif exp[0] == :method_add_arg
    call = convert(exp[1])
    call[:args].push(convert(exp[2]))
    call
  elsif [:call, :fcall, :command, :command_call, :super].include?(exp[0])
    if exp[2] == :"." or exp[2] == :"::"
      puts "value: #{exp[1]}, attr: #{exp[3]}"
      func = { 
        :type => :attribute,
        :value => convert(exp[1]),
        :attr => convert(exp[3])
      }
    else
      func = convert(exp[1])
    end
    {
      :type => exp[0],
      :func => func,
      :args => []
    }    
  elsif exp[0] == :args_new
    {
      :type => :args,
      :args => []
    }
  elsif exp[0] == :args_add
    args = convert(exp[1])
    args[:args].push(convert(exp[2]))
    args
  elsif exp[0] == :args_add_star
    args = convert(exp[1])
    if exp[2]
      args[:star] = convert(exp[2])
    end
    args
  elsif exp[0] == :args_add_block
    args = convert(exp[1])
    if exp[2]
      args[:block] = convert(exp[2])
    end
    args
  elsif exp[0] == :assign
    {
      :type => :assign,
      :target => convert(exp[1]),
      :value => convert(exp[2])
    }
  elsif exp[0] == :opassign
    # convert x+=1 into x=x+1
    operation = convert([:binary, exp[1], exp[2][1], exp[3]])
    {
      :type => :assign,
      :target => convert(exp[1]),
      :value => operation
    }
  elsif exp[0] == :dot2 or exp[0] == :dot3
    {
      :type => exp[0],
      :from => convert(exp[1]),
      :to => convert(exp[2])
    }
  elsif [:top_const_ref, :return, :yield, :defined].include?(exp[0])
    # constructs that contains one thing
    {
      :type => exp[0],
      :value => convert(exp[1])
    }
  elsif exp[0] == :alias
    {
      :type => :alias,
      :name1 => convert(exp[1]),
      :name2 => convert(exp[2])
    }
  elsif exp[0] == :undef
    {
      :type => :undef,
      :names => convert_array(exp[1]),
    }
  elsif exp[0] == :if or exp[0] == :if_mod or exp[0] == :elsif
    ret = {
      :type => :if,
      :test => convert(exp[1]),
      :body => convert(exp[2])
    }
    if exp[3]
      ret[:else] = convert(exp[3])
    end
    ret
  elsif exp[0] == :while or exp[0] == :while_mod
    {
      :type => :while,
      :test => convert(exp[1]),
      :body => convert(exp[2])
    }
  elsif exp[0] == :unless or exp[0] == :unless_mod
    # to be converted to 'if not test ...'
    ret = {
      :type => :unless,
      :test => convert(exp[1]),
      :body => convert(exp[2])
    }
    if exp[3]
      ret[:else] = convert(exp[3])
    end
    ret
  elsif exp[0] == :until or exp[0] == :until_mod
    # to be converted to 'while not test ...'
    {
      :type => :until,
      :test => convert(exp[1]),
      :body => convert(exp[2])
    }
  elsif exp[0] == :for
    {
      :type => :for,
      :var => convert(exp[1]),
      :in => convert(exp[2]),
      :body => convert(exp[3])
    }
  elsif exp[0] == :begin
    ## ??
    {
      :type => :begin,
      :s1 => convert(exp[1]),
      :in => convert(exp[2]),
      :body => convert(exp[3])
    }
  elsif exp[0] == :stmts_new
    {
      :type => :block,
      :stmts => []
    }
  elsif exp[0] == :stmts_add
    block = convert(exp[1])
    stmt = convert(exp[2])
    block[:stmts].push(stmt)
    block
  elsif exp[0] == :binary
    {
      :type => :binary,
      :left => convert(exp[1]),
      :op => op(exp[2]),
      :right => convert(exp[3])
    }
  elsif exp[0] == :unary
    {
      :type => :unary,
      :op => op(exp[1]),
      :value => convert(exp[2])
    }
  elsif exp[0] == :@int
    {
      :type => :int,
      :n => exp[1],
      :location => exp[2]
    }
  elsif exp[0] == :hash
    {
      :type => :hash,
      :value => convert(exp[1])
    }
  elsif exp[0] == :const_path_ref
    {
      :type => :attribute,
      :value => convert(exp[1]),
      :attr => convert(exp[2])
    }
  elsif exp[0] == :void_stmt
    {
      :type => :void
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


def make_keyword(arr)
  {
    :type => :keyword,
    :key => convert(arr[0]),
    :value => convert(arr[1])
  }
end


def op(name)
  {
    :type => :op,
    :name => name
  }
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
              :symbol_literal,
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

